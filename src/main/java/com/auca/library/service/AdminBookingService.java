package com.auca.library.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
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

    public List<BookingResponse> getCurrentBookings() {
        LocalDateTime now = LocalDateTime.now();
        List<Booking> currentBookings = bookingRepository.findByEndTimeAfterAndStartTimeBeforeAndStatusIn(
                now, now, List.of(BookingStatus.RESERVED, BookingStatus.CHECKED_IN));
        
        return currentBookings.stream()
                .map(this::mapBookingToResponse)
                .collect(Collectors.toList());
    }

    public List<BookingResponse> getBookingsByDate(LocalDate date) {
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(LocalTime.MAX);
        
        List<Booking> bookings = bookingRepository.findByStartTimeBetween(startOfDay, endOfDay);
        
        return bookings.stream()
                .map(this::mapBookingToResponse)
                .collect(Collectors.toList());
    }

    public List<BookingResponse> getBookingsInDateRange(LocalDate start, LocalDate end) {
        LocalDateTime startDateTime = start.atStartOfDay();
        LocalDateTime endDateTime = end.atTime(LocalTime.MAX);
        
        List<Booking> bookings = bookingRepository.findByStartTimeBetween(startDateTime, endDateTime);
        
        return bookings.stream()
                .map(this::mapBookingToResponse)
                .collect(Collectors.toList());
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
        
        // Find bookings that started more than 20 minutes ago but haven't been checked in
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

}