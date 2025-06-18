package com.auca.library.service;

import com.auca.library.dto.response.*;
import com.auca.library.exception.ResourceNotFoundException;
import com.auca.library.model.*;
import com.auca.library.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Service
public class QRScanService {

    @Autowired
    private SeatRepository seatRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private RoomBookingRepository roomBookingRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private QRCodeLogRepository qrCodeLogRepository;

    @Autowired
    private NotificationService notificationService;

    private static final int EARLY_CHECK_IN_MINUTES = 15;
    private static final int LATE_CHECK_IN_MINUTES = 20;

    /**
     * Process QR code scan
     */
    @Transactional
    public QRScanResponse processScan(String type, String token, String userEmail) {
        // Validate token format
        if (!isValidToken(token)) {
            return createErrorResponse("Invalid QR code format");
        }

        // Process based on type
        if ("seat".equalsIgnoreCase(type)) {
            return processSeatScan(token, userEmail);
        } else if ("room".equalsIgnoreCase(type)) {
            return processRoomScan(token, userEmail);
        } else {
            return createErrorResponse("Invalid QR code type");
        }
    }

    /**
     * Process seat QR code scan
     */
    private QRScanResponse processSeatScan(String token, String userEmail) {
        // Find seat by token
        Optional<Seat> seatOpt = seatRepository.findByQrCodeToken(token);
        if (seatOpt.isEmpty()) {
            return createErrorResponse("Invalid or expired QR code");
        }

        Seat seat = seatOpt.get();
        
        // Check if seat is disabled
        if (seat.isDisabled()) {
            return createErrorResponse("This seat is currently unavailable");
        }

        // Create base response
        QRScanResponse response = new QRScanResponse();
        response.setSuccess(true);
        response.setResourceType("SEAT");
        response.setResourceId(seat.getId());
        response.setResourceIdentifier(seat.getSeatNumber());
        response.setResourceDetails(createSeatDetails(seat));

        // If no user email (unauthenticated scan)
        if (userEmail == null || userEmail.isEmpty()) {
            response.setRequiresAuthentication(true);
            response.setMessage("Please log in to book or check in to this seat");
            response.setAvailabilityInfo(checkSeatAvailability(seat));
            return response;
        }

        // Find user
        Optional<User> userOpt = userRepository.findByEmail(userEmail);
        if (userOpt.isEmpty()) {
            response.setRequiresAuthentication(true);
            response.setMessage("User not found. Please log in again.");
            return response;
        }

        User user = userOpt.get();
        response.setUserId(user.getId());
        response.setUserName(user.getFullName());

        // Check for active booking
        LocalDateTime now = LocalDateTime.now();
        List<Booking> activeBookings = bookingRepository.findActiveBookingsByUserId(user.getId());
        
        Optional<Booking> relevantBooking = activeBookings.stream()
                .filter(b -> b.getSeat().getId().equals(seat.getId()))
                .filter(b -> isWithinBookingWindow(b, now))
                .findFirst();

        if (relevantBooking.isPresent()) {
            return handleSeatBookingCheckIn(relevantBooking.get(), response, now);
        } else {
            // No booking for this seat
            response.setAction("VIEW_AVAILABILITY");
            response.setMessage("You don't have a booking for this seat");
            response.setAvailabilityInfo(checkSeatAvailability(seat));
            response.setCanBook(true);
            
            // Check if user has booking for different seat at this time
            Optional<Booking> otherBooking = activeBookings.stream()
                    .filter(b -> !b.getSeat().getId().equals(seat.getId()))
                    .filter(b -> b.getStartTime().isBefore(now.plusMinutes(30)) && 
                               b.getEndTime().isAfter(now))
                    .findFirst();
            
            if (otherBooking.isPresent()) {
                response.setWarning("You have a booking for seat " + 
                    otherBooking.get().getSeat().getSeatNumber() + " at this time");
            }
        }

        return response;
    }

    /**
     * Process room QR code scan
     */
    private QRScanResponse processRoomScan(String token, String userEmail) {
        // Find room by token
        Optional<Room> roomOpt = roomRepository.findByQrCodeToken(token);
        if (roomOpt.isEmpty()) {
            return createErrorResponse("Invalid or expired QR code");
        }

        Room room = roomOpt.get();
        
        // Check if room is available
        if (!room.isAvailable()) {
            return createErrorResponse("This room is currently unavailable");
        }

        // Check if under maintenance
        if (room.isUnderMaintenance()) {
            return createErrorResponse("This room is under maintenance");
        }

        // Create base response
        QRScanResponse response = new QRScanResponse();
        response.setSuccess(true);
        response.setResourceType("ROOM");
        response.setResourceId(room.getId());
        response.setResourceIdentifier(room.getRoomNumber());
        response.setResourceDetails(createRoomDetails(room));

        // If no user email (unauthenticated scan)
        if (userEmail == null || userEmail.isEmpty()) {
            response.setRequiresAuthentication(true);
            response.setMessage("Please log in to book or check in to this room");
            response.setAvailabilityInfo(checkRoomAvailability(room));
            return response;
        }

        // Find user
        Optional<User> userOpt = userRepository.findByEmail(userEmail);
        if (userOpt.isEmpty()) {
            response.setRequiresAuthentication(true);
            response.setMessage("User not found. Please log in again.");
            return response;
        }

        User user = userOpt.get();
        response.setUserId(user.getId());
        response.setUserName(user.getFullName());

        // Check for active room booking
        LocalDateTime now = LocalDateTime.now();
        List<RoomBooking> activeBookings = roomBookingRepository.findByUserAndStatusIn(
            user, 
            List.of(RoomBooking.BookingStatus.CONFIRMED, RoomBooking.BookingStatus.CHECKED_IN)
        );
        
        Optional<RoomBooking> relevantBooking = activeBookings.stream()
                .filter(b -> b.getRoom().getId().equals(room.getId()))
                .filter(b -> isWithinRoomBookingWindow(b, now))
                .findFirst();

        if (relevantBooking.isPresent()) {
            return handleRoomBookingCheckIn(relevantBooking.get(), response, now);
        } else {
            // Check if user is participant in any booking for this room
            Optional<RoomBooking> participantBooking = findParticipantBooking(user, room, now);
            if (participantBooking.isPresent()) {
                return handleParticipantCheckIn(participantBooking.get(), user, response, now);
            }
            
            // No booking for this room
            response.setAction("VIEW_AVAILABILITY");
            response.setMessage("You don't have a booking for this room");
            response.setAvailabilityInfo(checkRoomAvailability(room));
            response.setCanBook(room.requiresBooking());
        }

        return response;
    }

    /**
     * Handle seat booking check-in
     */
    private QRScanResponse handleSeatBookingCheckIn(Booking booking, QRScanResponse response, LocalDateTime now) {
        // Already checked in
        if (booking.getStatus() == Booking.BookingStatus.CHECKED_IN) {
            response.setAction("ALREADY_CHECKED_IN");
            response.setMessage("You are already checked in");
            response.setCheckInTime(booking.getCheckedInTime());
            response.setBookingDetails(createBookingDetails(booking));
            return response;
        }

        // Check timing
        LocalDateTime bookingStart = booking.getStartTime();
        LocalDateTime bookingEnd = booking.getEndTime();
        
        if (now.isBefore(bookingStart.minusMinutes(EARLY_CHECK_IN_MINUTES))) {
            // Too early
            long minutesUntilStart = java.time.Duration.between(now, bookingStart).toMinutes();
            response.setAction("TOO_EARLY");
            response.setMessage(String.format("Check-in opens %d minutes before your booking", EARLY_CHECK_IN_MINUTES));
            response.setWarning(String.format("Please come back in %d minutes", 
                minutesUntilStart - EARLY_CHECK_IN_MINUTES));
            response.setBookingDetails(createBookingDetails(booking));
            return response;
        }

        if (now.isAfter(bookingStart.plusMinutes(LATE_CHECK_IN_MINUTES))) {
            // Too late - booking might be cancelled
            response.setAction("TOO_LATE");
            response.setMessage("Check-in window has expired");
            response.setWarning("Your booking may have been cancelled due to no-show");
            response.setBookingDetails(createBookingDetails(booking));
            return response;
        }

        // Valid check-in time
        response.setAction("CHECK_IN");
        response.setMessage("Ready to check in");
        response.setCanCheckIn(true);
        response.setBookingDetails(createBookingDetails(booking));
        
        if (now.isBefore(bookingStart)) {
            long minutesEarly = java.time.Duration.between(now, bookingStart).toMinutes();
            response.setInfo(String.format("You are checking in %d minutes early", minutesEarly));
        }

        return response;
    }

    /**
     * Handle room booking check-in
     */
    private QRScanResponse handleRoomBookingCheckIn(RoomBooking booking, QRScanResponse response, LocalDateTime now) {
        // Already checked in
        if (booking.getCheckedInAt() != null) {
            response.setAction("ALREADY_CHECKED_IN");
            response.setMessage("You are already checked in");
            response.setCheckInTime(booking.getCheckedInAt());
            response.setBookingDetails(createRoomBookingDetails(booking));
            return response;
        }

        // Check if booking requires check-in
        if (!booking.isRequiresCheckIn()) {
            response.setAction("NO_CHECK_IN_REQUIRED");
            response.setMessage("This booking does not require check-in");
            response.setBookingDetails(createRoomBookingDetails(booking));
            return response;
        }

        // Check timing
        if (!booking.canCheckIn()) {
            LocalDateTime bookingStart = booking.getStartTime();
            
            if (now.isBefore(bookingStart.minusMinutes(EARLY_CHECK_IN_MINUTES))) {
                long minutesUntilStart = java.time.Duration.between(now, bookingStart).toMinutes();
                response.setAction("TOO_EARLY");
                response.setMessage("Check-in not yet available");
                response.setWarning(String.format("Check-in opens in %d minutes", 
                    minutesUntilStart - EARLY_CHECK_IN_MINUTES));
            } else if (now.isAfter(booking.getEndTime())) {
                response.setAction("BOOKING_ENDED");
                response.setMessage("This booking has already ended");
            }
            response.setBookingDetails(createRoomBookingDetails(booking));
            return response;
        }

        // Valid check-in
        response.setAction("CHECK_IN");
        response.setMessage("Ready to check in");
        response.setCanCheckIn(true);
        response.setBookingDetails(createRoomBookingDetails(booking));
        
        // Add participant count
        int checkedInCount = booking.getCheckedInCount();
        int totalParticipants = booking.getParticipants().size() + 1;
        response.setInfo(String.format("%d of %d participants checked in", checkedInCount, totalParticipants));

        return response;
    }

    /**
     * Handle participant check-in for room booking
     */
    private QRScanResponse handleParticipantCheckIn(RoomBooking booking, User user, QRScanResponse response, LocalDateTime now) {
        // Find participant record
        BookingParticipant participant = booking.getParticipants().stream()
                .filter(p -> p.getUser().getId().equals(user.getId()))
                .findFirst()
                .orElse(null);

        if (participant == null || participant.getStatus() != BookingParticipant.ParticipantStatus.ACCEPTED) {
            response.setAction("NOT_AUTHORIZED");
            response.setMessage("You are not an accepted participant for this booking");
            return response;
        }

        // Already checked in
        if (participant.getCheckedInAt() != null) {
            response.setAction("ALREADY_CHECKED_IN");
            response.setMessage("You are already checked in as a participant");
            response.setCheckInTime(participant.getCheckedInAt());
            response.setBookingDetails(createRoomBookingDetails(booking));
            return response;
        }

        // Check timing
        if (!booking.canCheckIn()) {
            response.setAction("NOT_CHECK_IN_TIME");
            response.setMessage("Check-in is not available at this time");
            response.setBookingDetails(createRoomBookingDetails(booking));
            return response;
        }

        // Valid participant check-in
        response.setAction("PARTICIPANT_CHECK_IN");
        response.setMessage("Ready to check in as participant");
        response.setCanCheckIn(true);
        response.setBookingDetails(createRoomBookingDetails(booking));
        response.setInfo("You are a participant in " + booking.getUser().getFullName() + "'s booking");

        return response;
    }

    /**
     * Create error response
     */
    private QRScanResponse createErrorResponse(String message) {
        QRScanResponse response = new QRScanResponse();
        response.setSuccess(false);
        response.setMessage(message);
        response.setAction("ERROR");
        return response;
    }

    /**
     * Check if token is valid UUID format
     */
    private boolean isValidToken(String token) {
        if (token == null || token.trim().isEmpty()) {
            return false;
        }
        try {
            java.util.UUID.fromString(token);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Check if current time is within booking window
     */
    private boolean isWithinBookingWindow(Booking booking, LocalDateTime now) {
        return now.isAfter(booking.getStartTime().minusMinutes(EARLY_CHECK_IN_MINUTES)) &&
               now.isBefore(booking.getEndTime());
    }

    /**
     * Check if current time is within room booking window
     */
    private boolean isWithinRoomBookingWindow(RoomBooking booking, LocalDateTime now) {
        return now.isAfter(booking.getStartTime().minusMinutes(EARLY_CHECK_IN_MINUTES)) &&
               now.isBefore(booking.getEndTime());
    }

    /**
     * Find if user is participant in any booking for the room
     */
    private Optional<RoomBooking> findParticipantBooking(User user, Room room, LocalDateTime now) {
        List<RoomBooking> roomBookings = roomBookingRepository.findActiveBookingsForRoom(
            room, now.minusMinutes(EARLY_CHECK_IN_MINUTES), now.plusHours(1)
        );
        
        return roomBookings.stream()
                .filter(b -> b.getParticipants().stream()
                        .anyMatch(p -> p.getUser().getId().equals(user.getId()) &&
                                     p.getStatus() == BookingParticipant.ParticipantStatus.ACCEPTED))
                .findFirst();
    }

    /**
     * Create seat details for response
     */
    private SeatDetailsResponse createSeatDetails(Seat seat) {
        SeatDetailsResponse details = new SeatDetailsResponse();
        details.setSeatId(seat.getId());
        details.setSeatNumber(seat.getSeatNumber());
        details.setZoneType(seat.getZoneType());
        details.setHasDesktop(seat.isHasDesktop());
        details.setDescription(seat.getDescription());
        return details;
    }

    /**
     * Create room details for response
     */
    private RoomDetailsResponse createRoomDetails(Room room) {
        RoomDetailsResponse details = new RoomDetailsResponse();
        details.setRoomId(room.getId());
        details.setRoomNumber(room.getRoomNumber());
        details.setRoomName(room.getName());
        details.setCategory(room.getCategory().name());
        details.setCapacity(room.getCapacity());
        details.setBuilding(room.getBuilding());
        details.setFloor(room.getFloor());
        details.setRequiresApproval(room.isRequiresApproval());
        return details;
    }

    /**
     * Create booking details for response
     */
    private BookingDetailsResponse createBookingDetails(Booking booking) {
        BookingDetailsResponse details = new BookingDetailsResponse();
        details.setBookingId(booking.getId());
        details.setStartTime(booking.getStartTime());
        details.setEndTime(booking.getEndTime());
        details.setStatus(booking.getStatus().name());
        details.setNotes(booking.getNotes());
        return details;
    }

    /**
     * Create room booking details for response
     */
    private RoomBookingDetailsResponse createRoomBookingDetails(RoomBooking booking) {
        RoomBookingDetailsResponse details = new RoomBookingDetailsResponse();
        details.setBookingId(booking.getId());
        details.setTitle(booking.getTitle());
        details.setStartTime(booking.getStartTime());
        details.setEndTime(booking.getEndTime());
        details.setStatus(booking.getStatus().name());
        details.setOrganizerName(booking.getUser().getFullName());
        details.setTotalParticipants(booking.getParticipants().size() + 1);
        details.setCheckedInCount(booking.getCheckedInCount());
        return details;
    }

    /**
     * Check seat availability
     */
    private String checkSeatAvailability(Seat seat) {
        LocalDateTime now = LocalDateTime.now();
        List<Booking> upcomingBookings = bookingRepository.findBySeat(seat).stream()
                .filter(b -> b.getEndTime().isAfter(now))
                .filter(b -> b.getStatus() == Booking.BookingStatus.RESERVED || 
                           b.getStatus() == Booking.BookingStatus.CHECKED_IN)
                .sorted((b1, b2) -> b1.getStartTime().compareTo(b2.getStartTime()))
                .toList();

        if (upcomingBookings.isEmpty()) {
            return "Seat is available";
        }

        Booking nextBooking = upcomingBookings.get(0);
        if (nextBooking.getStartTime().isBefore(now.plusMinutes(30))) {
            return String.format("Occupied until %s", 
                nextBooking.getEndTime().format(DateTimeFormatter.ofPattern("HH:mm")));
        } else {
            return String.format("Available until %s", 
                nextBooking.getStartTime().format(DateTimeFormatter.ofPattern("HH:mm")));
        }
    }

    /**
     * Check room availability
     */
    private String checkRoomAvailability(Room room) {
        LocalDateTime now = LocalDateTime.now();
        Optional<RoomBooking> currentBooking = roomBookingRepository.findCurrentBookingForRoom(room, now);
        
        if (currentBooking.isPresent()) {
            return String.format("Occupied until %s by %s", 
                currentBooking.get().getEndTime().format(DateTimeFormatter.ofPattern("HH:mm")),
                currentBooking.get().getTitle());
        }

        Optional<RoomBooking> nextBooking = roomBookingRepository.findNextBookingForRoom(room, now);
        if (nextBooking.isPresent() && nextBooking.get().getStartTime().isBefore(now.plusHours(2))) {
            return String.format("Available until %s", 
                nextBooking.get().getStartTime().format(DateTimeFormatter.ofPattern("HH:mm")));
        }

        return "Room is available";
    }

    /**
     * Log QR code scan
     */
    @Transactional
    public void logScan(String type, String token, String userEmail, boolean success) {
        // TODO: Implement scan logging for analytics
        // This would create a scan log entry for tracking usage patterns
    }
}