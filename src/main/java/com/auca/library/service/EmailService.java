// Update EmailService.java
package com.auca.library.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;
    
    private static final DateTimeFormatter DATE_TIME_FORMATTER = 
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @Async
    public void sendVerificationEmail(String to, String token) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true);
        
        helper.setTo(to);
        helper.setSubject("Verify your AUCA Library Account");
        
        String verificationUrl = "http://localhost:8080/api/auth/verify?token=" + token;
        
        String content = "<html><body>"
                + "<h2>AUCA Library Account Verification</h2>"
                + "<p>Thank you for registering. Please click the link below to verify your account:</p>"
                + "<a href='" + verificationUrl + "'>Verify your account</a>"
                + "<p>This link will expire in 24 hours.</p>"
                + "</body></html>";
        
        helper.setText(content, true);
        
        mailSender.send(message);
    }
    
    @Async
    public void sendBookingConfirmation(String to, String seatNumber, LocalDateTime startTime, 
                                     LocalDateTime endTime) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true);
        
        helper.setTo(to);
        helper.setSubject("Your AUCA Library Seat Booking Confirmation");
        
        String content = "<html><body>"
                + "<h2>AUCA Library Seat Booking Confirmation</h2>"
                + "<p>Your seat booking has been confirmed with the following details:</p>"
                + "<ul>"
                + "<li><strong>Seat Number:</strong> " + seatNumber + "</li>"
                + "<li><strong>Start Time:</strong> " + startTime.format(DATE_TIME_FORMATTER) + "</li>"
                + "<li><strong>End Time:</strong> " + endTime.format(DATE_TIME_FORMATTER) + "</li>"
                + "</ul>"
                + "<p>Please arrive on time and check in using the library booking system.</p>"
                + "</body></html>";
        
        helper.setText(content, true);
        
        mailSender.send(message);
    }
    
    @Async
    public void sendExtensionNotification(String to, String seatNumber, Long bookingId) 
            throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true);
        
        helper.setTo(to);
        helper.setSubject("Your AUCA Library Booking is Ending Soon");
        
        String confirmUrl = "http://localhost:3000/bookings/extend/" + bookingId + "?extend=true";
        String declineUrl = "http://localhost:3000/bookings/extend/" + bookingId + "?extend=false";
        
        String content = "<html><body>"
                + "<h2>AUCA Library Booking Notification</h2>"
                + "<p>Your booking for seat " + seatNumber + " is ending in 10 minutes.</p>"
                + "<p>Would you like to extend your booking by 1 hour?</p>"
                + "<p>"
                + "<a href='" + confirmUrl + "' style='background-color: #4CAF50; color: white; "
                + "padding: 10px 15px; text-decoration: none; margin-right: 10px;'>Yes, Extend</a>"
                + "<a href='" + declineUrl + "' style='background-color: #f44336; color: white; "
                + "padding: 10px 15px; text-decoration: none;'>No, Thank You</a>"
                + "</p>"
                + "<p>Please respond within 5 minutes. If you don't respond, your booking will end as scheduled.</p>"
                + "</body></html>";
        
        helper.setText(content, true);
        
        mailSender.send(message);
    }
    
    @Async
    public void sendWaitListNotification(String to, String seatNumber, LocalDateTime startTime,
                                     LocalDateTime endTime) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true);
        
        helper.setTo(to);
        helper.setSubject("Your Waitlisted AUCA Library Seat is Available");
        
        String bookingUrl = "http://localhost:3000/seats";
        
        String content = "<html><body>"
                + "<h2>AUCA Library Wait List Notification</h2>"
                + "<p>Good news! The seat you were waiting for is now available:</p>"
                + "<ul>"
                + "<li><strong>Seat Number:</strong> " + seatNumber + "</li>"
                + "<li><strong>Time Period:</strong> " + startTime.format(DATE_TIME_FORMATTER) 
                + " to " + endTime.format(DATE_TIME_FORMATTER) + "</li>"
                + "</ul>"
                + "<p>Please visit the booking system to reserve your seat before someone else does:</p>"
                + "<a href='" + bookingUrl + "' style='background-color: #4CAF50; color: white; "
                + "padding: 10px 15px; text-decoration: none;'>Book Now</a>"
                + "</body></html>";
        
        helper.setText(content, true);
        
        mailSender.send(message);
    }



    /**
 * Send a warning email when user hasn't checked in after 10 minutes
 */
@Async
public void sendCheckInWarning(String to, String seatNumber, LocalDateTime startTime) 
        throws MessagingException {
    MimeMessage message = mailSender.createMimeMessage();
    MimeMessageHelper helper = new MimeMessageHelper(message, true);
    
    helper.setTo(to);
    helper.setSubject("URGENT: Check-in Required for Your Library Booking");
    
    String checkInUrl = "http://localhost:3000/my-bookings";
    
    String content = "<html><body>"
            + "<h2>AUCA Library Check-in Reminder</h2>"
            + "<p style='color: #e74c3c; font-weight: bold;'>Your booking requires immediate check-in!</p>"
            + "<p>You have a booking for seat <strong>" + seatNumber + "</strong> starting at <strong>" 
            + startTime.format(DATE_TIME_FORMATTER) + "</strong>.</p>"
            + "<p>Please check in within the next 10 minutes or your booking will be automatically cancelled.</p>"
            + "<p><a href='" + checkInUrl + "' style='background-color: #e74c3c; color: white; "
            + "padding: 10px 15px; text-decoration: none;'>Check In Now</a></p>"
            + "<p>If you no longer need this seat, please cancel your booking to make it available for others.</p>"
            + "</body></html>";
    
    helper.setText(content, true);
    
    mailSender.send(message);
}
}