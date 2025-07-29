package com.auca.library.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.auca.library.dto.response.BookingDTO;
import com.auca.library.dto.response.BookingResponse;
import com.auca.library.dto.response.MessageResponse;
import com.auca.library.exception.ResourceNotFoundException;
import com.auca.library.model.Booking;
import com.auca.library.model.Booking.BookingStatus;
import com.auca.library.model.Location;
import com.auca.library.model.WaitList;
import com.auca.library.repository.BookingRepository;
import com.auca.library.repository.WaitListRepository;

import jakarta.mail.MessagingException;

@Service
public class AdminBookingService {

    @Autowired
    private BookingRepository bookingRepository;

     @Autowired
    private BookingService bookingService;

    @Autowired
    private EmailService emailService;
    
    @Autowired
    private WaitListRepository waitListRepository;

    @Autowired
    private NotificationService notificationService;

    public List<BookingResponse> getCurrentBookings(Location location) {
        LocalDate today = LocalDate.now();
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.atTime(LocalTime.MAX);

        List<Booking> bookings;

         
         if (location != null) {
              bookings = bookingRepository.findTodaysActiveBookingsByLocation(
                location, startOfDay, endOfDay,
                List.of(BookingStatus.RESERVED, BookingStatus.CHECKED_IN)
             );
           } else {
               bookings = bookingRepository.findTodaysActiveBookings(
               startOfDay, endOfDay,
              List.of(BookingStatus.RESERVED, BookingStatus.CHECKED_IN)
            );
        }
        
           return bookings.stream()
            .map(this::mapBookingToResponse)
            .collect(Collectors.toList());
    }

public List<BookingResponse> getBookingsByDate(LocalDate date) {
    // Create start and end of day LocalDateTime objects
    LocalDateTime startOfDay = date.atStartOfDay();
    LocalDateTime startOfNextDay = date.plusDays(1).atStartOfDay();
    
    List<Booking> bookings = bookingRepository.findBookingsByDate(startOfDay, startOfNextDay);
    
    return bookings.stream()
            .map(this::mapBookingToResponse)
            .collect(Collectors.toList());
}

public List<BookingResponse> getBookingsInDateRange(LocalDate start, LocalDate end) {
    LocalDateTime startDateTime = start.atStartOfDay();
    LocalDateTime endDateTime = end.plusDays(1).atStartOfDay(); // Include the end date
    
    List<Booking> bookings = bookingRepository.findBookingsByDateRange(startDateTime, endDateTime);
    
    return bookings.stream()
            .map(this::mapBookingToResponse)
            .collect(Collectors.toList());
}

public List<BookingResponse> getTodaysActiveBookings() {
    LocalDate today = LocalDate.now();
    LocalDateTime startOfDay = today.atStartOfDay();
    LocalDateTime endOfDay = today.plusDays(1).atStartOfDay();
    
    List<BookingStatus> activeStatuses = List.of(BookingStatus.RESERVED, BookingStatus.CHECKED_IN);
    
    List<Booking> bookings = bookingRepository.findTodaysActiveBookings(startOfDay, endOfDay, activeStatuses);
    
    return bookings.stream()
            .map(this::mapBookingToResponse)
            .collect(Collectors.toList());
}


public Map<String, Long> getTodaysBookingStats() {
    LocalDate today = LocalDate.now();
    LocalDateTime startOfDay = today.atStartOfDay();
    LocalDateTime endOfDay = today.plusDays(1).atStartOfDay();
    
    List<Object[]> results = bookingRepository.countTodaysBookingsByStatus(startOfDay, endOfDay);
    
    Map<String, Long> stats = new HashMap<>();
    for (Object[] result : results) {
        BookingStatus status = (BookingStatus) result[0];
        Long count = (Long) result[1];
        stats.put(status.name(), count);
    }
    
    return stats;
}

    @Transactional
    public MessageResponse cancelBooking(Long id) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id: " + id));
        
        booking.setStatus(BookingStatus.CANCELLED);
        booking.setCancellationTime(LocalDateTime.now());
        booking.setCancellationReason("Cancelled by System");
        
        bookingRepository.save(booking);
        return new MessageResponse("Booking cancelled successfully");
    }

    public List<BookingResponse> getBookingsByUser(Long userId) {
        List<Booking> bookings = bookingRepository.findByUserId(userId);
        
        return bookings.stream()
                .map(this::mapBookingToResponse)
                .collect(Collectors.toList());
    }

    public List<BookingResponse> getBookingsBySeat(Long seatId) {
        List<Booking> bookings = bookingRepository.findBySeatId(seatId);
        
        return bookings.stream()
                .map(this::mapBookingToResponse)
                .collect(Collectors.toList());
    }

    // Helper method for mapping Booking to BookingResponse
    private BookingResponse mapBookingToResponse(Booking booking) {
        BookingResponse response = new BookingResponse();
        response.setId(booking.getId());
        response.setUserId(booking.getUser().getId());
        response.setUserName(booking.getUser().getFullName());
        response.setIdentifier(booking.getUser().getIdentifier());
        response.setSeatId(booking.getSeat().getId());
        response.setSeatNumber(booking.getSeat().getSeatNumber());
        response.setStartTime(booking.getStartTime());
        response.setEndTime(booking.getEndTime());
        response.setStatus(booking.getStatus().name());
        response.setCheckinTime(booking.getCheckinTime());
        response.setCheckoutTime(booking.getCheckoutTime());
        response.setCancellationTime(booking.getCancellationTime());
        response.setCancellationReason(booking.getCancellationReason());
        return response;
    }



     
    @Scheduled(fixedRate = 60000) // Runs every minute
    public void checkForNoShowBookings() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime cutoffTime = now.minusMinutes(7);
        
        // Find bookings that started more than x minutes ago but haven't been checked in
        List<Booking> noShowBookings = bookingRepository.findNoShowBookings(cutoffTime, now);
        
        for (Booking booking : noShowBookings) {
            // Mark as no-show and release the seat
            markAsNoShow(booking.getId(), "Automatic cancellation due to no-show after 20 minutes");
        }
    }
    
    @Transactional
public BookingDTO markAsNoShow(Long id, String cancellationReason) {
    Booking booking = bookingRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id: " + id));
    
    // Only mark as no-show if it's still in RESERVED status
    if (booking.getStatus() != Booking.BookingStatus.RESERVED) {
        return createBookingDTO(booking);
    }
    
    booking.setStatus(Booking.BookingStatus.NO_SHOW);
    booking.setCancellationTime(LocalDateTime.now());
    booking.setCancellationReason(cancellationReason != null ? cancellationReason : "Marked as no-show by System");
    booking = bookingRepository.save(booking);
    
    // Send no-show notification via NotificationService
    notificationService.sendNoShowNotification(
        booking.getUser(),
        booking.getSeat().getSeatNumber(),
        booking.getStartTime()
    );
    
    // Notify waitlist users 
    notifyWaitListUsers(booking.getSeat().getId(), booking.getStartTime(), booking.getEndTime());
    
    return createBookingDTO(booking);
}


     private BookingDTO createBookingDTO(Booking booking) {
        BookingDTO dto = new BookingDTO();
        dto.setId(booking.getId());
        dto.setUserId(booking.getUser().getId());
        dto.setUserName(booking.getUser().getFullName());
        dto.setIdentifier(booking.getUser().getIdentifier());
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
        // Set other fields as needed...
        return dto;
    }
    
    // Create our own waitlist notification method since the original is private
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

                 // Send notification via NotificationService
                notificationService.sendWaitListNotification(
                waitItem.getUser(),
                waitItem.getSeat(),
                waitItem.getRequestedStartTime(),
                waitItem.getRequestedEndTime()
            );
                
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
    
    // Helper method to check time overlap
    private boolean isTimeOverlapping(LocalDateTime start1, LocalDateTime end1, 
                                LocalDateTime start2, LocalDateTime end2) {
        return start1.isBefore(end2) && end1.isAfter(start2);
    }
    
     

    
 // Manual check-in by admin

@Transactional
public BookingResponse manualCheckIn(Long bookingId, String adminEmail) {
    Booking booking = bookingRepository.findById(bookingId)
            .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id: " + bookingId));
    
    // Validate booking can be checked in
    if (booking.getStatus() != BookingStatus.RESERVED) {
        throw new IllegalStateException("Only reserved bookings can be checked in. Current status: " + booking.getStatus());
    }
    
    LocalDateTime now = LocalDateTime.now();
    
    // Check if booking time has started (optional validation)
    if (now.isBefore(booking.getStartTime().minusMinutes(15))) {
        throw new IllegalStateException("Cannot check in more than 15 minutes before booking start time");
    }
    
    // Perform check-in
    booking.setStatus(BookingStatus.CHECKED_IN);
    booking.setCheckinTime(now);
    booking.setCheckedIn(true);
    booking.setCheckedInTime(now);

    
    booking = bookingRepository.save(booking);
    
    // Send notification to user
    notificationService.sendCheckInConfirmation(
        booking.getUser(),
        booking.getSeat().getSeatNumber(),
        booking.getStartTime(),
        true // isAdminAction
    );
    
    return mapBookingToResponse(booking);
}


 // Manual cancellation by admin with optional reason
@Transactional
public BookingResponse manualCancelBooking(Long bookingId, String reason, String adminEmail) {
    Booking booking = bookingRepository.findById(bookingId)
            .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id: " + bookingId));
    
    // Only cancel if not already cancelled or completed
    if (booking.getStatus() == BookingStatus.CANCELLED || 
        booking.getStatus() == BookingStatus.COMPLETED ||
        booking.getStatus() == BookingStatus.NO_SHOW) {
        throw new IllegalStateException("Cannot cancel booking with status: " + booking.getStatus());
    }
    
    // Store original status for notification
    BookingStatus originalStatus = booking.getStatus();
    
    // Cancel the booking
    booking.setStatus(BookingStatus.CANCELLED);
    booking.setCancellationTime(LocalDateTime.now());
    booking.setCancellationReason(reason != null && !reason.trim().isEmpty() 
        ? "Admin cancellation: " + reason.trim()
        : "Cancelled by admin: " + adminEmail);
    
    // If user was checked in, also set checkout time
    if (originalStatus == BookingStatus.CHECKED_IN) {
        booking.setCheckoutTime(LocalDateTime.now());
        // booking.setCheckedOut(true);
    }
    
    booking = bookingRepository.save(booking);
    
    // Send notification to user about cancellation
    notificationService.sendBookingCancellationNotification(
        booking.getUser(),
        booking.getSeat().getSeatNumber(),
        booking.getStartTime(),
        booking.getCancellationReason(),
        true // isAdminAction
    );
    
    // Notify waitlist users about the newly available seat
    notifyWaitListUsers(booking.getSeat().getId(), booking.getStartTime(), booking.getEndTime());
    
    return mapBookingToResponse(booking);
}


 // Bulk cancellation by admin
@Transactional
public BulkCancellationResponse bulkCancelBookings(List<Long> bookingIds, String reason, String adminEmail) {
    List<BookingResponse> cancelledBookings = new ArrayList<>();
    List<BulkCancellationError> errors = new ArrayList<>();
    
    for (Long bookingId : bookingIds) {
        try {
            BookingResponse cancelled = manualCancelBooking(bookingId, reason, adminEmail);
            cancelledBookings.add(cancelled);
        } catch (Exception e) {
            errors.add(new BulkCancellationError(bookingId, e.getMessage()));
        }
    }
    
    return new BulkCancellationResponse(
        cancelledBookings.size(),
        errors.size(),
        cancelledBookings,
        errors,
        reason
    );
}


 // Get bookings that can be manually checked in (reserved bookings within check-in window)
 
public List<BookingResponse> getBookingsEligibleForCheckIn() {
    LocalDateTime now = LocalDateTime.now();
    LocalDateTime earliestCheckIn = now.minusMinutes(15); // Can check in up to 15 minutes early
    LocalDateTime latestCheckIn = now.plusMinutes(30);    // Grace period for late check-ins
    
    List<Booking> eligibleBookings = bookingRepository.findBookingsEligibleForCheckIn(
        earliestCheckIn, latestCheckIn, BookingStatus.RESERVED
    );
    
    return eligibleBookings.stream()
            .map(this::mapBookingToResponse)
            .collect(Collectors.toList());
}


 // Get bookings that can be cancelled (not already cancelled/completed)

public List<BookingResponse> getBookingsEligibleForCancellation() {
    List<BookingStatus> cancellableStatuses = List.of(
        BookingStatus.RESERVED, 
        BookingStatus.CHECKED_IN
    );
    
    List<Booking> eligibleBookings = bookingRepository.findByStatusIn(cancellableStatuses);
    
    return eligibleBookings.stream()
            .map(this::mapBookingToResponse)
            .collect(Collectors.toList());
}

// Helper classes for bulk operations
public static class BulkCancellationResponse {
    private int successCount;
    private int errorCount;
    private List<BookingResponse> cancelledBookings;
    private List<BulkCancellationError> errors;
    private String reason;
    
    // Constructors, getters, setters
    public BulkCancellationResponse(int successCount, int errorCount, 
                                   List<BookingResponse> cancelledBookings,
                                   List<BulkCancellationError> errors, String reason) {
        this.successCount = successCount;
        this.errorCount = errorCount;
        this.cancelledBookings = cancelledBookings;
        this.errors = errors;
        this.reason = reason;
    }
    
    // Getters and setters
    public int getSuccessCount() { return successCount; }
    public void setSuccessCount(int successCount) { this.successCount = successCount; }
    public int getErrorCount() { return errorCount; }
    public void setErrorCount(int errorCount) { this.errorCount = errorCount; }
    public List<BookingResponse> getCancelledBookings() { return cancelledBookings; }
    public void setCancelledBookings(List<BookingResponse> cancelledBookings) { this.cancelledBookings = cancelledBookings; }
    public List<BulkCancellationError> getErrors() { return errors; }
    public void setErrors(List<BulkCancellationError> errors) { this.errors = errors; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}

public static class BulkCancellationError {
    private Long bookingId;
    private String errorMessage;
    
    public BulkCancellationError(Long bookingId, String errorMessage) {
        this.bookingId = bookingId;
        this.errorMessage = errorMessage;
    }
    
    // Getters and setters
    public Long getBookingId() { return bookingId; }
    public void setBookingId(Long bookingId) { this.bookingId = bookingId; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
}
}