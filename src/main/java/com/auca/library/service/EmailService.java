package com.auca.library.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

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
        
        String verificationUrl = "http://192.168.1.68:3000/verify?token=" + token;
        
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
        
        String confirmUrl = "http://http://192.168.1.68:3000/bookings";

        
        String content = "<html><body>"
                + "<h2>AUCA Library Booking Notification</h2>"
                + "<p>Your booking for seat " + seatNumber + " is ending in 10 minutes.</p>"
                + "<p>Would you like to extend your booking by 1 hour?</p>"
                + "<p>"
                + "<a href='" + confirmUrl + "' style='background-color: #4CAF50; color: white; "
                + "padding: 10px 15px; text-decoration: none; margin-right: 10px;'>Extend</a>"
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

@Async
public void sendNoShowNotification(String to, String seatNumber, LocalDateTime startTime) 
        throws MessagingException {
    MimeMessage message = mailSender.createMimeMessage();
    MimeMessageHelper helper = new MimeMessageHelper(message, true);
    
    helper.setTo(to);
    helper.setSubject("AUCA Library Booking Cancelled - No Show");
    
    String bookingUrl = "http://localhost:3000/seats";
    
    String content = "<html><body>"
            + "<h2>AUCA Library Booking Cancellation Notice</h2>"
            + "<p>Your booking has been automatically cancelled because you did not check in within 20 minutes of the start time:</p>"
            + "<ul>"
            + "<li><strong>Seat Number:</strong> " + seatNumber + "</li>"
            + "<li><strong>Start Time:</strong> " + startTime.format(DATE_TIME_FORMATTER) + "</li>"
            + "</ul>"
            + "<p>The seat is now available for other users.</p>"
            + "<p>If you still need a seat, you can make a new booking:</p>"
            + "<a href='" + bookingUrl + "' style='background-color: #4CAF50; color: white; "
            + "padding: 10px 15px; text-decoration: none;'>Book Again</a>"
            + "</body></html>";
    
    helper.setText(content, true);
    
    mailSender.send(message);
}
    
}