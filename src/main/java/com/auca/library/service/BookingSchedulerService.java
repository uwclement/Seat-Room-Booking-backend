package com.auca.library.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.auca.library.model.Booking;
import com.auca.library.model.User;
import com.auca.library.repository.BookingRepository;
import com.auca.library.util.NotificationConstants;

@Service
public class BookingSchedulerService {
    
    @Autowired
    private BookingRepository bookingRepository;
    
    @Autowired
    private AdminBookingService adminBookingService;
    
    @Autowired
    private NotificationService notificationService;
    
    @Autowired
    private EmailService emailService;
    
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    
    /**
     * Run every minute to check for bookings that need check-in warnings (10 minutes after start time)
     */
    @Scheduled(fixedRate = 60000) // Every minute
    @Transactional
    public void checkForCheckInWarnings() {
        LocalDateTime now = LocalDateTime.now();
        
        // Find bookings that started 10 minutes ago and haven't been checked in yet
        LocalDateTime warningCutoff = now.minusMinutes(2);
        
        // Cut off at 19 minutes to avoid overlap with the no-show check
        LocalDateTime maxCutoff = now.minusMinutes(5);
        
        List<Booking> bookingsNeedingWarning = bookingRepository.findBookingsNeedingWarning(
            warningCutoff, maxCutoff);
        
        for (Booking booking : bookingsNeedingWarning) {
            // Send warning notification
            sendCheckInWarning(booking);
        }
    }
    
    /**
     * Run every minute to check for no-show bookings (20 minutes after start time)
     */
    @Scheduled(fixedRate = 60000) // Every minute
    @Transactional
    public void checkForNoShowBookings() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime cutoffTime = now.minusMinutes(7);
        
        // Find bookings that started more than 20 minutes ago but haven't been checked in
        List<Booking> noShowBookings = bookingRepository.findNoShowBookings(cutoffTime, now);
        
        for (Booking booking : noShowBookings) {
            // Mark as no-show and release the seat
            adminBookingService.markAsNoShow(booking.getId(), 
                "Automatic cancellation due to no-show after 20 minutes");
        }
    }
    
    /**
     * Send a warning notification when a user hasn't checked in 10 minutes after booking start
     */
    private void sendCheckInWarning(Booking booking) {
        User user = booking.getUser();
        
        // Send in-app notification (if you have implemented the NotificationService)
        try {
            // Create warning message
            String message = "Your booking for seat " + booking.getSeat().getSeatNumber() + 
                " started at " + booking.getStartTime().format(TIME_FORMATTER) + 
                ". Please check in within the next 10 minutes or your booking will be automatically cancelled.";
            
            notificationService.addNotification(
                user.getEmail(),
                "Check-in Required Soon",
                message,
                NotificationConstants.TYPE_CHECK_IN_WARNING
            );
             booking.setWarningSent(true); // Mark warning as sent
             bookingRepository.save(booking);
        } catch (Exception e) {
            // Log error but continue processing
            System.err.println("Failed to send check-in warning notification: " + e.getMessage());
        }
        
        // Send email warning
        // try {
        //     emailService.sendCheckInWarning(
        //         user.getEmail(),
        //         booking.getSeat().getSeatNumber(),
        //         booking.getStartTime());
        // } catch (MessagingException e) {
        //     System.err.println("Failed to send check-in warning email: " + e.getMessage());
        // }
    }
}