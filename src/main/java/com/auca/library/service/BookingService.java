package com.auca.library.service;

import com.auca.library.dto.request.CreateBookingRequest;
import com.auca.library.dto.request.ExtensionRequest;
import com.auca.library.dto.response.BookingDTO;
import com.auca.library.exception.BadRequestException;
import com.auca.library.exception.ResourceNotFoundException;
import com.auca.library.model.Booking;
import com.auca.library.model.Seat;
import com.auca.library.model.User;
import com.auca.library.model.WaitList;
import com.auca.library.repository.BookingRepository;
import com.auca.library.repository.SeatRepository;
import com.auca.library.repository.UserRepository;
import com.auca.library.repository.WaitListRepository;
import jakarta.mail.MessagingException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class BookingService {

    @Autowired
    private BookingRepository bookingRepository;
    
    @Autowired
    private SeatRepository seatRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private WaitListRepository waitListRepository;
    
    @Autowired
    private SeatService seatService;
    
    @Autowired
    private EmailService emailService;
    
    private static final int MAX_BOOKING_HOURS = 6;
    
    @Transactional
    public BookingDTO createBooking(CreateBookingRequest request) throws MessagingException {
        // Get current user
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUserEmail = authentication.getName();
        
        User user = userRepository.findByEmail(currentUserEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + currentUserEmail));
        
        Seat seat = seatRepository.findById(request.getSeatId())
                .orElseThrow(() -> new ResourceNotFoundException("Seat not found with id: " + request.getSeatId()));
        
        if (seat.isDisabled()) {
            throw new BadRequestException("This seat is currently unavailable for booking");
        }
        
        // Validate booking time constraints
        validateBookingTime(request.getStartTime(), request.getEndTime(), user.getId());
        
        // Check if seat is available during requested time
        if (!seatService.isSeatAvailable(seat.getId(), request.getStartTime(), request.getEndTime())) {
            throw new BadRequestException("The seat is not available for the requested time period");
        }
        
        // Create new booking
        Booking booking = new Booking(user, seat, request.getStartTime(), request.getEndTime());
        booking.setNotes(request.getNotes());
        
        booking = bookingRepository.save(booking);
        
        // Check if there are people on the wait list for this seat
        // and the booking time overlaps with their requested time
        List<WaitList> waitingUsers = waitListRepository.findWaitingListForSeat(seat.getId());
        for (WaitList waitItem : waitingUsers) {
            if (isTimeOverlapping(waitItem.getRequestedStartTime(), waitItem.getRequestedEndTime(), 
                                 request.getStartTime(), request.getEndTime())) {
                // Remove this person from the wait list since the seat is now booked
                waitItem.setStatus(WaitList.WaitListStatus.FULFILLED);
                waitListRepository.save(waitItem);
            }
        }
        
        return mapBookingToDTO(booking);
    }
    
    public List<BookingDTO> getCurrentUserBookings() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUserEmail = authentication.getName();
        
        User user = userRepository.findByEmail(currentUserEmail)
        .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + currentUserEmail));

LocalDateTime now = LocalDateTime.now();

// Get all bookings for this user
List<Booking> bookings = bookingRepository.findByUser(user);

// Filter for active and upcoming bookings
return bookings.stream()
        .filter(b -> b.getEndTime().isAfter(now) && 
                     (b.getStatus() == Booking.BookingStatus.RESERVED || 
                      b.getStatus() == Booking.BookingStatus.CHECKED_IN))
        .map(this::mapBookingToDTO)
        .collect(Collectors.toList());
}

public List<BookingDTO> getPastBookings() {
Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
String currentUserEmail = authentication.getName();

User user = userRepository.findByEmail(currentUserEmail)
        .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + currentUserEmail));

LocalDateTime now = LocalDateTime.now();

// Get past bookings
List<Booking> pastBookings = bookingRepository.findPastBookingsByUser(user.getId(), now);

return pastBookings.stream()
        .map(this::mapBookingToDTO)
        .collect(Collectors.toList());
}

public BookingDTO getBookingById(Long id) {
Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
String currentUserEmail = authentication.getName();

User user = userRepository.findByEmail(currentUserEmail)
        .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + currentUserEmail));

Booking booking = bookingRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id: " + id));

// Make sure the booking belongs to the current user
if (!booking.getUser().getId().equals(user.getId())) {
    throw new BadRequestException("You don't have permission to view this booking");
}

return mapBookingToDTO(booking);
}

@Transactional
public BookingDTO cancelBooking(Long id) {
Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
String currentUserEmail = authentication.getName();

User user = userRepository.findByEmail(currentUserEmail)
        .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + currentUserEmail));

Booking booking = bookingRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id: " + id));

// Make sure the booking belongs to the current user
if (!booking.getUser().getId().equals(user.getId())) {
    throw new BadRequestException("You don't have permission to cancel this booking");
}

// Check if booking is already completed or cancelled
if (booking.getStatus() == Booking.BookingStatus.COMPLETED || 
    booking.getStatus() == Booking.BookingStatus.CANCELLED) {
    throw new BadRequestException("This booking cannot be cancelled");
}

booking.setStatus(Booking.BookingStatus.CANCELLED);
booking = bookingRepository.save(booking);

// Check if there are people on the wait list for this seat
notifyWaitListUsers(booking.getSeat().getId(), booking.getStartTime(), booking.getEndTime());

return mapBookingToDTO(booking);
}

@Transactional
public BookingDTO checkIn(Long id) {
Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
String currentUserEmail = authentication.getName();

User user = userRepository.findByEmail(currentUserEmail)
        .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + currentUserEmail));

Booking booking = bookingRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id: " + id));

// Make sure the booking belongs to the current user
if (!booking.getUser().getId().equals(user.getId())) {
    throw new BadRequestException("You don't have permission to check in to this booking");
}

// Check if booking is in RESERVED status
if (booking.getStatus() != Booking.BookingStatus.RESERVED) {
    throw new BadRequestException("This booking cannot be checked in");
}

// Update booking status
booking.setStatus(Booking.BookingStatus.CHECKED_IN);
booking.setCheckedIn(true);
booking.setCheckedInTime(LocalDateTime.now());

booking = bookingRepository.save(booking);

return mapBookingToDTO(booking);
}

@Transactional
public BookingDTO checkOut(Long id) {
Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
String currentUserEmail = authentication.getName();

User user = userRepository.findByEmail(currentUserEmail)
        .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + currentUserEmail));

Booking booking = bookingRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id: " + id));

// Make sure the booking belongs to the current user
if (!booking.getUser().getId().equals(user.getId())) {
    throw new BadRequestException("You don't have permission to check out of this booking");
}

// Check if booking is in CHECKED_IN status
if (booking.getStatus() != Booking.BookingStatus.CHECKED_IN) {
    throw new BadRequestException("This booking cannot be checked out");
}

// Update booking status
booking.setStatus(Booking.BookingStatus.COMPLETED);
booking.setCheckedOutTime(LocalDateTime.now());

booking = bookingRepository.save(booking);

// Check if there are people on the wait list for this seat
notifyWaitListUsers(booking.getSeat().getId(), booking.getStartTime(), booking.getEndTime());

return mapBookingToDTO(booking);
}

@Transactional
public BookingDTO respondToExtension(ExtensionRequest request) {
Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
String currentUserEmail = authentication.getName();

User user = userRepository.findByEmail(currentUserEmail)
        .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + currentUserEmail));

Booking booking = bookingRepository.findById(request.getBookingId())
        .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id: " + request.getBookingId()));

// Make sure the booking belongs to the current user
if (!booking.getUser().getId().equals(user.getId())) {
    throw new BadRequestException("You don't have permission to extend this booking");
}

// Check if booking is in CHECKED_IN status and extension was requested
if (booking.getStatus() != Booking.BookingStatus.CHECKED_IN || !booking.isExtensionRequested()) {
    throw new BadRequestException("This booking cannot be extended");
}

// Check if extension response is within 5 minutes of notification
LocalDateTime now = LocalDateTime.now();
if (booking.getExtensionNotifiedAt() == null || 
    Duration.between(booking.getExtensionNotifiedAt(), now).toMinutes() > 5) {
    throw new BadRequestException("Extension time has expired");
}

booking.setExtensionRespondedAt(now);

if (request.isExtend()) {
    // Check if the seat is available for the extension period
    LocalDateTime newEndTime = booking.getEndTime().plusHours(1);
    
    if (!seatService.isSeatAvailable(booking.getSeat().getId(), booking.getEndTime(), newEndTime)) {
        throw new BadRequestException("The seat is not available for extension");
    }
    
    booking.setExtended(true);
    booking.setEndTime(newEndTime);
}

booking = bookingRepository.save(booking);

return mapBookingToDTO(booking);
}

private void notifyWaitListUsers(Long seatId, LocalDateTime startTime, LocalDateTime endTime) {
// Find users waiting for this seat with overlapping time
List<WaitList> waitingList = waitListRepository.findWaitingListForSeat(seatId);

for (WaitList waitItem : waitingList) {
    if (isTimeOverlapping(waitItem.getRequestedStartTime(), waitItem.getRequestedEndTime(), 
                         startTime, endTime) && !waitItem.isNotified()) {
        
        // Update wait list item
        waitItem.setNotified(true);
        waitItem.setNotifiedAt(LocalDateTime.now());
        waitItem.setStatus(WaitList.WaitListStatus.NOTIFIED);
        waitListRepository.save(waitItem);
        
        // Send notification email
        try {
            emailService.sendWaitListNotification(
                waitItem.getUser().getEmail(),
                waitItem.getSeat().getSeatNumber(),
                waitItem.getRequestedStartTime(),
                waitItem.getRequestedEndTime());
        } catch (MessagingException e) {
            // Log error but continue processing
            System.err.println("Failed to send wait list notification: " + e.getMessage());
        }
    }
}
}

private boolean isTimeOverlapping(LocalDateTime start1, LocalDateTime end1, 
                            LocalDateTime start2, LocalDateTime end2) {
return start1.isBefore(end2) && end1.isAfter(start2);
}

private void validateBookingTime(LocalDateTime startTime, LocalDateTime endTime, Long userId) {
LocalDateTime now = LocalDateTime.now();

// Check if start time is in the future
if (startTime.isBefore(now)) {
    throw new BadRequestException("Start time must be in the future");
}

// Check if end time is after start time
if (endTime.isBefore(startTime)) {
    throw new BadRequestException("End time must be after start time");
}

// Check maximum booking duration (6 hours)
Duration duration = Duration.between(startTime, endTime);
if (duration.toHours() > MAX_BOOKING_HOURS) {
    throw new BadRequestException("Booking duration cannot exceed " + MAX_BOOKING_HOURS + " hours");
}

// Check if booking is within the current week (or next week from Sunday)
LocalDate today = LocalDate.now();
LocalDate nextSunday = today.with(TemporalAdjusters.next(DayOfWeek.SUNDAY));
LocalDate nextWeekSunday = nextSunday.plusWeeks(1);

// If today is Sunday, allow booking for next week
boolean isSunday = today.getDayOfWeek() == DayOfWeek.SUNDAY;
LocalDate maxBookingDate = isSunday ? nextWeekSunday : nextSunday;

if (startTime.toLocalDate().isAfter(maxBookingDate)) {
    throw new BadRequestException("Bookings can only be made for the current week" + 
                                (isSunday ? " or next week" : ""));
}

// Check if user already has an active booking for overlapping time
List<Booking> activeBookings = bookingRepository.findActiveBookingsByUserId(userId);

for (Booking booking : activeBookings) {
    if (isTimeOverlapping(booking.getStartTime(), booking.getEndTime(), startTime, endTime)) {
        throw new BadRequestException("You already have an active booking during this time period");
    }
}
}

private BookingDTO mapBookingToDTO(Booking booking) {
BookingDTO dto = new BookingDTO();

dto.setId(booking.getId());
dto.setUserId(booking.getUser().getId());
dto.setUserName(booking.getUser().getFullName());
dto.setSeatId(booking.getSeat().getId());
dto.setSeatNumber(booking.getSeat().getSeatNumber());
dto.setZoneType(booking.getSeat().getZoneType());
dto.setStartTime(booking.getStartTime());
dto.setEndTime(booking.getEndTime());
dto.setCreatedAt(booking.getCreatedAt());
dto.setStatus(booking.getStatus());
dto.setCheckedIn(booking.isCheckedIn());
dto.setCheckedInTime(booking.getCheckedInTime());
dto.setCheckedOutTime(booking.getCheckedOutTime());
dto.setExtended(booking.isExtended());
dto.setExtensionRequested(booking.isExtensionRequested());
dto.setExtensionNotifiedAt(booking.getExtensionNotifiedAt());
dto.setNotes(booking.getNotes());

// Calculate remaining minutes if booking is active
if (booking.getStatus() == Booking.BookingStatus.CHECKED_IN) {
    LocalDateTime now = LocalDateTime.now();
    if (now.isBefore(booking.getEndTime())) {
        dto.setRemainingMinutes(Duration.between(now, booking.getEndTime()).toMinutes());
    } else {
        dto.setRemainingMinutes(0);
    }
}

return dto;
}
}