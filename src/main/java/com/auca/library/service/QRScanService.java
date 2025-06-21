package com.auca.library.service;

import com.auca.library.dto.response.*;
import com.auca.library.exception.ResourceNotFoundException;
import com.auca.library.model.*;
import com.auca.library.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

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
 * Process seat QR code scan - ENHANCED VERSION
 */
private QRScanResponse processSeatScan(String token, String userEmail) {
    // Find seat by token
    Optional<Seat> seatOpt = seatRepository.findByQrCodeToken(token);
    if (seatOpt.isEmpty()) {
        return createErrorResponse("INVALID_QR", "Invalid or expired QR code");
    }

    Seat seat = seatOpt.get();
    
    // Check if seat is disabled
    if (seat.isDisabled()) {
        return createErrorResponse("SEAT_UNAVAILABLE", "This seat is currently unavailable");
    }

    // Create base response
    QRScanResponse response = new QRScanResponse();
    response.setSuccess(true);
    response.setResourceType("SEAT");
    response.setResourceId(seat.getId());
    response.setResourceIdentifier(seat.getSeatNumber());
    response.setResourceDetails(createSeatDetails(seat));

    // Handle unauthenticated users
    if (userEmail == null || userEmail.isEmpty()) {
        response.setRequiresAuthentication(true);
        response.setMessage("Please log in to check in to this seat");
        response.setAvailabilityInfo(checkSeatAvailability(seat));
        response.setQrScanContext(createScanContext(seat, token));
        response.setActionButtonText("Log In");
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

    LocalDateTime now = LocalDateTime.now();
    
    // CORE LOGIC: Check for booking on THIS specific seat
    Optional<Booking> specificSeatBooking = findUserBookingForSpecificSeat(user, seat, now);

    if (specificSeatBooking.isPresent()) {
        return handleValidBookingCheckIn(specificSeatBooking.get(), response, now);
    } else {
        return handleNoBookingForThisSeat(user, seat, response, now);
    }
}

/**
 * Handle case where user has valid booking for scanned seat
 */
private QRScanResponse handleValidBookingCheckIn(Booking booking, QRScanResponse response, LocalDateTime now) {
    // Already checked in
    if (booking.getStatus() == Booking.BookingStatus.CHECKED_IN) {
        response.setAction("ALREADY_CHECKED_IN");
        response.setMessage("You are already checked in to this seat");
        response.setCheckInTime(booking.getCheckedInTime());
        response.setBookingDetails(createBookingDetails(booking));
        response.setActionButtonText("Already Checked In");
        return response;
    }

    LocalDateTime bookingStart = booking.getStartTime();
    LocalDateTime checkInWindowStart = bookingStart.minusMinutes(10);
    LocalDateTime checkInWindowEnd = bookingStart.plusMinutes(10);
    
    // Set booking time info
    response.setBookingStartTime(bookingStart);
    response.setBookingEndTime(booking.getEndTime());
    response.setBookingDetails(createBookingDetails(booking));
    
    if (now.isBefore(checkInWindowStart)) {
        // Too early - check-in window hasn't opened
        long minutesUntilCheckIn = Duration.between(now, checkInWindowStart).toMinutes();
        response.setSuccess(false);
        response.setAction("TOO_EARLY");
        response.setErrorCode("CHECK_IN_NOT_OPEN");
        response.setMessage("Check-in opens 10 minutes before your booking");
        response.setWarning(String.format("Please come back in %d minutes", minutesUntilCheckIn));
        response.setCheckInAvailableAt(checkInWindowStart);
        response.setActionButtonText("Set Reminder");
        response.setInfo(String.format("Your booking starts at %s", 
            bookingStart.format(DateTimeFormatter.ofPattern("HH:mm"))));
        return response;
    }

    if (now.isAfter(checkInWindowEnd)) {
        // Too late - check-in window has closed
        response.setSuccess(false);
        response.setAction("TOO_LATE");
        response.setErrorCode("CHECK_IN_EXPIRED");
        response.setMessage("Check-in window has expired");
        response.setWarning("Your booking may have been cancelled due to no-show");
        response.setAlternativeAction("CONTACT_SUPPORT");
        response.setActionButtonText("Contact Support");
        response.setSecondaryButtonText("Book Again");
        return response;
    }

    // Perfect timing - allow check-in
    response.setAction("CHECK_IN");
    response.setMessage("Ready to check in");
    response.setCanCheckIn(true);
    response.setActionButtonText("Check In Now");
    
    if (now.isBefore(bookingStart)) {
        long minutesEarly = Duration.between(now, bookingStart).toMinutes();
        response.setInfo(String.format("Checking in %d minutes early", minutesEarly));
    }

    return response;
}

/**
 * Handle case where user has no booking for the scanned seat
 */
private QRScanResponse handleNoBookingForThisSeat(User user, Seat seat, QRScanResponse response, LocalDateTime now) {
    response.setSuccess(false);
    response.setAction("NO_BOOKING");
    response.setErrorCode("NO_BOOKING_FOR_SEAT");
    response.setMessage("You don't have a scheduled booking for this seat");
    response.setCanCheckIn(false);
    
    // Check if user has booking for different seat
    Optional<Booking> otherBooking = findUserBookingForOtherSeat(user, seat, now);
    if (otherBooking.isPresent()) {
        Booking booking = otherBooking.get();
        response.setWarning(String.format(
            "You have a booking for Seat %s from %s to %s", 
            booking.getSeat().getSeatNumber(),
            booking.getStartTime().format(DateTimeFormatter.ofPattern("HH:mm")),
            booking.getEndTime().format(DateTimeFormatter.ofPattern("HH:mm"))
        ));
        response.setAlternativeAction("GO_TO_BOOKED_SEAT");
        response.setAlternativeDetails(createBookingDetails(booking));
        response.setActionButtonText("Go to My Seat");
        response.setSecondaryButtonText("View My Bookings");
    } else {
        // No bookings at all - suggest booking this seat
        response.setActionButtonText("Book This Seat");
        response.setSecondaryButtonText("Find Available Seats");
    }
    
    // Show seat availability
    response.setAvailabilityInfo(checkSeatAvailability(seat));
    response.setCanBook(isSeatAvailableForBooking(seat, now));
    
    return response;
}

/**
 * error response
 */
private QRScanResponse createErrorResponse(String errorCode, String message) {
    QRScanResponse response = new QRScanResponse();
    response.setSuccess(false);
    response.setErrorCode(errorCode);
    response.setMessage(message);
    response.setAction("ERROR");
    response.setActionButtonText("Try Again");
    return response;
}

   
/**
 * Process room QR code scan 
 */
private QRScanResponse processRoomScan(String token, String userEmail) {
    // Find room by token
    Optional<Room> roomOpt = roomRepository.findByQrCodeToken(token);
    if (roomOpt.isEmpty()) {
        return createErrorResponse("INVALID_QR", "Invalid or expired QR code");
    }

    Room room = roomOpt.get();
    
    // Check if room is available
    if (!room.isAvailable()) {
        return createErrorResponse("ROOM_UNAVAILABLE", "This room is currently unavailable");
    }

    // Check if under maintenance
    if (room.isUnderMaintenance()) {
        return createErrorResponse("UNDER_MAINTENANCE", "This room is under maintenance");
    }

    // Create base response
    QRScanResponse response = new QRScanResponse();
    response.setSuccess(true);
    response.setResourceType("ROOM");
    response.setResourceId(room.getId());
    response.setResourceIdentifier(room.getRoomNumber());
    response.setResourceDetails(createRoomDetails(room));

    // Handle unauthenticated users
    if (userEmail == null || userEmail.isEmpty()) {
        response.setRequiresAuthentication(true);
        response.setMessage("Please log in to check in to this room");
        response.setAvailabilityInfo(checkRoomAvailability(room));
        response.setQrScanContext(createScanContext(room, token));
        response.setActionButtonText("Log In");
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

    LocalDateTime now = LocalDateTime.now();
    
    // CORE LOGIC: Check for booking on THIS specific room
    Optional<RoomBooking> specificRoomBooking = findUserBookingForSpecificRoom(user, room, now);

    if (specificRoomBooking.isPresent()) {
        return handleValidRoomBookingCheckIn(specificRoomBooking.get(), response, now);
    } else {
        // Check if user is participant in any booking for this room
        Optional<RoomBooking> participantBooking = findParticipantBookingForRoom(user, room, now);
        if (participantBooking.isPresent()) {
            return handleParticipantRoomCheckIn(participantBooking.get(), user, response, now);
        }
        
        return handleNoBookingForThisRoom(user, room, response, now);
    }
}

/**
 * Handle case where user has valid room booking for scanned room
 */
private QRScanResponse handleValidRoomBookingCheckIn(RoomBooking booking, QRScanResponse response, LocalDateTime now) {
    // Already checked in
    if (booking.getCheckedInAt() != null) {
        response.setAction("ALREADY_CHECKED_IN");
        response.setMessage("You are already checked in to this room");
        response.setCheckInTime(booking.getCheckedInAt());
        response.setBookingDetails(createRoomBookingDetails(booking));
        response.setActionButtonText("Already Checked In");
        return response;
    }

    // Check if booking requires check-in
    if (!booking.isRequiresCheckIn()) {
        response.setAction("NO_CHECK_IN_REQUIRED");
        response.setMessage("This booking does not require check-in");
        response.setBookingDetails(createRoomBookingDetails(booking));
        response.setActionButtonText("Enter Room");
        return response;
    }

    LocalDateTime bookingStart = booking.getStartTime();
    LocalDateTime checkInWindowStart = bookingStart.minusMinutes(10);
    LocalDateTime checkInWindowEnd = bookingStart.plusMinutes(10);
    
    // Set booking time info
    response.setBookingStartTime(bookingStart);
    response.setBookingEndTime(booking.getEndTime());
    response.setBookingDetails(createRoomBookingDetails(booking));
    
    if (now.isBefore(checkInWindowStart)) {
        // Too early - check-in window hasn't opened
        long minutesUntilCheckIn = Duration.between(now, checkInWindowStart).toMinutes();
        response.setSuccess(false);
        response.setAction("TOO_EARLY");
        response.setErrorCode("CHECK_IN_NOT_OPEN");
        response.setMessage("Check-in opens 10 minutes before your booking");
        response.setWarning(String.format("Please come back in %d minutes", minutesUntilCheckIn));
        response.setCheckInAvailableAt(checkInWindowStart);
        response.setActionButtonText("Set Reminder");
        response.setInfo(String.format("Your booking starts at %s", 
            bookingStart.format(DateTimeFormatter.ofPattern("HH:mm"))));
        return response;
    }

    if (now.isAfter(checkInWindowEnd)) {
        // Too late - check-in window has closed
        response.setSuccess(false);
        response.setAction("TOO_LATE");
        response.setErrorCode("CHECK_IN_EXPIRED");
        response.setMessage("Check-in window has expired");
        response.setWarning("Your booking may have been cancelled due to no-show");
        response.setAlternativeAction("CONTACT_SUPPORT");
        response.setActionButtonText("Contact Support");
        response.setSecondaryButtonText("Book Again");
        return response;
    }

    // Perfect timing - allow check-in
    response.setAction("CHECK_IN");
    response.setMessage("Ready to check in");
    response.setCanCheckIn(true);
    response.setActionButtonText("Check In Now");
    
    // Add participant count info
    int checkedInCount = booking.getCheckedInCount();
    int totalParticipants = booking.getParticipants().size() + 1;
    response.setInfo(String.format("%d of %d participants checked in", checkedInCount, totalParticipants));
    
    if (now.isBefore(bookingStart)) {
        long minutesEarly = Duration.between(now, bookingStart).toMinutes();
        response.setInfo(response.getInfo() + String.format(" (Checking in %d minutes early)", minutesEarly));
    }

    return response;
}

/**
 * Handle participant check-in for room booking
 */
private QRScanResponse handleParticipantRoomCheckIn(RoomBooking booking, User user, QRScanResponse response, LocalDateTime now) {
    // Find participant record
    BookingParticipant participant = booking.getParticipants().stream()
            .filter(p -> p.getUser().getId().equals(user.getId()))
            .findFirst()
            .orElse(null);

    if (participant == null || participant.getStatus() != BookingParticipant.ParticipantStatus.ACCEPTED) {
        response.setSuccess(false);
        response.setAction("NOT_AUTHORIZED");
        response.setErrorCode("NOT_PARTICIPANT");
        response.setMessage("You are not an accepted participant for this booking");
        response.setActionButtonText("Contact Organizer");
        return response;
    }

    // Already checked in
    if (participant.getCheckedInAt() != null) {
        response.setAction("ALREADY_CHECKED_IN");
        response.setMessage("You are already checked in as a participant");
        response.setCheckInTime(participant.getCheckedInAt());
        response.setBookingDetails(createRoomBookingDetails(booking));
        response.setActionButtonText("Already Checked In");
        return response;
    }

    // Check timing using same 10-minute window
    LocalDateTime bookingStart = booking.getStartTime();
    LocalDateTime checkInWindowStart = bookingStart.minusMinutes(10);
    LocalDateTime checkInWindowEnd = bookingStart.plusMinutes(10);
    
    if (now.isBefore(checkInWindowStart) || now.isAfter(checkInWindowEnd)) {
        response.setSuccess(false);
        response.setAction("NOT_CHECK_IN_TIME");
        response.setErrorCode("CHECK_IN_WINDOW_CLOSED");
        response.setMessage("Check-in window is not open");
        response.setBookingDetails(createRoomBookingDetails(booking));
        response.setActionButtonText("Wait for Check-in");
        return response;
    }

    // Valid participant check-in
    response.setAction("PARTICIPANT_CHECK_IN");
    response.setMessage("Ready to check in as participant");
    response.setCanCheckIn(true);
    response.setBookingDetails(createRoomBookingDetails(booking));
    response.setInfo("You are a participant in " + booking.getUser().getFullName() + "'s booking");
    response.setActionButtonText("Check In as Participant");

    return response;
}

/**
 * Handle case where user has no booking for the scanned room
 */
private QRScanResponse handleNoBookingForThisRoom(User user, Room room, QRScanResponse response, LocalDateTime now) {
    response.setSuccess(false);
    response.setAction("NO_BOOKING");
    response.setErrorCode("NO_BOOKING_FOR_ROOM");
    response.setMessage("You don't have a scheduled booking for this room");
    response.setCanCheckIn(false);
    
    // Check if user has booking for different room
    Optional<RoomBooking> otherBooking = findUserBookingForOtherRoom(user, room, now);
    if (otherBooking.isPresent()) {
        RoomBooking booking = otherBooking.get();
        response.setWarning(String.format(
            "You have a booking for %s from %s to %s", 
            booking.getRoom().getRoomNumber(),
            booking.getStartTime().format(DateTimeFormatter.ofPattern("HH:mm")),
            booking.getEndTime().format(DateTimeFormatter.ofPattern("HH:mm"))
        ));
        response.setAlternativeAction("GO_TO_BOOKED_ROOM");
        response.setAlternativeDetails(createRoomBookingDetails(booking));
        response.setActionButtonText("Go to My Room");
        response.setSecondaryButtonText("View My Bookings");
    } else {
        // No bookings at all - suggest booking this room
        response.setActionButtonText("Book This Room");
        response.setSecondaryButtonText("Find Available Rooms");
    }
    
    // Show room availability
    response.setAvailabilityInfo(checkRoomAvailability(room));
    response.setCanBook(isRoomAvailableForBooking(room, now));
    
    return response;
}

/**
 * Find if user is participant in any booking for the room
 */
private Optional<RoomBooking> findParticipantBookingForRoom(User user, Room room, LocalDateTime now) {
    List<RoomBooking> roomBookings = roomBookingRepository.findActiveBookingsForRoom(
        room, now.minusMinutes(10), now.plusHours(1)
    );
    
    return roomBookings.stream()
            .filter(b -> b.getParticipants().stream()
                    .anyMatch(p -> p.getUser().getId().equals(user.getId()) &&
                                 p.getStatus() == BookingParticipant.ParticipantStatus.ACCEPTED))
            .filter(b -> isWithinRoomCheckInWindow(b, now))
            .findFirst();
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
 * Log QR code scan - ASYNC VERSION to avoid transaction rollback
 */
@Async
@Transactional(propagation = Propagation.REQUIRES_NEW)
public void logScanAsync(String type, String token, String userEmail, boolean success) {
    try {
        QRCodeLog log = new QRCodeLog();
        log.setResourceType(type.toUpperCase());
        log.setNewToken(token);
        log.setGenerationReason(success ? "SCAN_SUCCESS" : "SCAN_FAILED");
        
        // FIXED: Handle null user email properly
        if (userEmail != null) {
            Optional<User> userOpt = userRepository.findByEmail(userEmail);
            if (userOpt.isPresent()) {
                log.setGeneratedBy(userOpt.get());
            } else {
                // For anonymous scans, create a placeholder or skip logging
                return; // Skip logging for invalid users
            }
        } else {
            // For anonymous scans, skip logging or create system user
            return; // Skip logging for anonymous scans
        }
        
        // Set a placeholder resource ID (you might want to get the actual ID)
        log.setResourceId(0L);
        
        qrCodeLogRepository.save(log);
    } catch (Exception e) {
        // Log error but don't throw exception
        System.err.println("Failed to log QR scan asynchronously: " + e.getMessage());
    }
}
/**
 * Log QR code scan 
 */
@Transactional
public void logScan(String type, String token, String userEmail, boolean success) {
    // For now, just call async version
    logScanAsync(type, token, userEmail, success);
}

/**
 * Find user's booking specifically for this seat within check-in window
 */
private Optional<Booking> findUserBookingForSpecificSeat(User user, Seat seat, LocalDateTime now) {
    return bookingRepository.findActiveBookingsByUserId(user.getId()).stream()
        .filter(b -> b.getSeat().getId().equals(seat.getId()))
        .filter(b -> isWithinCheckInWindow(b, now))
        .findFirst();
}

/**
 * Check if current time is within the 10-minute check-in window
 */
private boolean isWithinCheckInWindow(Booking booking, LocalDateTime now) {
    LocalDateTime checkInStart = booking.getStartTime().minusMinutes(10);
    LocalDateTime checkInEnd = booking.getStartTime().plusMinutes(10);
    return now.isAfter(checkInStart) && now.isBefore(checkInEnd);
}

/**
 * Find user's booking for a different seat (to show helpful message)
 */
private Optional<Booking> findUserBookingForOtherSeat(User user, Seat scannedSeat, LocalDateTime now) {
    return bookingRepository.findActiveBookingsByUserId(user.getId()).stream()
        .filter(b -> !b.getSeat().getId().equals(scannedSeat.getId()))
        .filter(b -> isBookingActiveOrUpcoming(b, now))
        .findFirst();
}

/**
 * Check if booking is active or upcoming today
 */
private boolean isBookingActiveOrUpcoming(Booking booking, LocalDateTime now) {
    return booking.getEndTime().isAfter(now) && 
           booking.getStartTime().toLocalDate().equals(now.toLocalDate());
}

/**
 * Create QR scan context for post-login processing
 */
private QRScanContext createScanContext(Seat seat, String token) {
    QRScanContext context = new QRScanContext();
    context.setToken(token);
    context.setType("seat");
    context.setResourceIdentifier(seat.getSeatNumber());
    context.setScannedAt(LocalDateTime.now());
    return context;
}

/**
 * Create QR scan context for rooms
 */
private QRScanContext createScanContext(Room room, String token) {
    QRScanContext context = new QRScanContext();
    context.setToken(token);
    context.setType("room");
    context.setResourceIdentifier(room.getRoomNumber());
    context.setScannedAt(LocalDateTime.now());
    return context;
}

/**
 * Check if seat is available for new booking
 */
private boolean isSeatAvailableForBooking(Seat seat, LocalDateTime now) {
    // Check if seat has any bookings in the next 2 hours
    List<Booking> upcomingBookings = bookingRepository.findBySeat(seat).stream()
            .filter(b -> b.getStartTime().isAfter(now) && 
                        b.getStartTime().isBefore(now.plusHours(2)))
            .filter(b -> b.getStatus() == Booking.BookingStatus.RESERVED || 
                        b.getStatus() == Booking.BookingStatus.CHECKED_IN)
            .collect(Collectors.toList());
    
    return upcomingBookings.isEmpty();
}

/**
 * Helper method to find user by email
 */
private User findUserByEmail(String email) {
    return userRepository.findByEmail(email)
            .orElseThrow(() -> new ResourceNotFoundException("User not found: " + email));
}


/**
 * Find user's booking specifically for this room within check-in window
 */
private Optional<RoomBooking> findUserBookingForSpecificRoom(User user, Room room, LocalDateTime now) {
    return roomBookingRepository.findByUserAndStatusIn(
        user, 
        List.of(RoomBooking.BookingStatus.CONFIRMED, RoomBooking.BookingStatus.CHECKED_IN)
    ).stream()
        .filter(b -> b.getRoom().getId().equals(room.getId()))
        .filter(b -> isWithinRoomCheckInWindow(b, now))
        .findFirst();
}

/**
 * Check if current time is within the 10-minute check-in window for rooms
 */
private boolean isWithinRoomCheckInWindow(RoomBooking booking, LocalDateTime now) {
    LocalDateTime checkInStart = booking.getStartTime().minusMinutes(10);
    LocalDateTime checkInEnd = booking.getStartTime().plusMinutes(10);
    return now.isAfter(checkInStart) && now.isBefore(checkInEnd);
}

/**
 * Find user's booking for a different room (to show helpful message)
 */
private Optional<RoomBooking> findUserBookingForOtherRoom(User user, Room scannedRoom, LocalDateTime now) {
    return roomBookingRepository.findByUserAndStatusIn(
        user, 
        List.of(RoomBooking.BookingStatus.CONFIRMED, RoomBooking.BookingStatus.CHECKED_IN)
    ).stream()
        .filter(b -> !b.getRoom().getId().equals(scannedRoom.getId()))
        .filter(b -> isRoomBookingActiveOrUpcoming(b, now))
        .findFirst();
}

/**
 * Check if room booking is active or upcoming today
 */
private boolean isRoomBookingActiveOrUpcoming(RoomBooking booking, LocalDateTime now) {
    return booking.getEndTime().isAfter(now) && 
           booking.getStartTime().toLocalDate().equals(now.toLocalDate());
}

/**
 * Check if room is available for new booking
 */
private boolean isRoomAvailableForBooking(Room room, LocalDateTime now) {
    Optional<RoomBooking> currentBooking = roomBookingRepository.findCurrentBookingForRoom(room, now);
    if (currentBooking.isPresent()) {
        return false;
    }
    
    Optional<RoomBooking> nextBooking = roomBookingRepository.findNextBookingForRoom(room, now);
    return nextBooking.isEmpty() || nextBooking.get().getStartTime().isAfter(now.plusHours(2));
}

}