package com.auca.library.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.auca.library.dto.request.NotificationMessage;
import com.auca.library.dto.response.MessageResponse;
import com.auca.library.service.NotificationService;
import com.auca.library.util.NotificationConstants;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    @Autowired
    private NotificationService notificationService;
    
    /**
     * GET /api/notifications
     * Retrieve all notifications for the current user
     */
    @GetMapping
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<List<NotificationMessage>> getCurrentUserNotifications() {
        List<NotificationMessage> notifications = notificationService.getCurrentUserNotifications();
        return ResponseEntity.ok(notifications);
    }
    
    /**
     * POST /api/notifications/{id}/read
     * Mark a specific notification as read
     * CHANGE: id is now Long instead of String
     */
    @PostMapping("/{id}/read")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<MessageResponse> markAsRead(@PathVariable Long id) {
        notificationService.markAsRead(id);
        return ResponseEntity.ok(new MessageResponse("Notification marked as read"));
    }
    
    /**
     * POST /api/notifications/mark-all-read
     * Mark all notifications as read for the current user
     */
    @PostMapping("/mark-all-read")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<MessageResponse> markAllAsRead() {
        notificationService.markAllAsRead();
        return ResponseEntity.ok(new MessageResponse("All notifications marked as read"));
    }


    
    /**
     * GET /api/notifications/unread
     * Get only unread notifications for current user
     * NEW ENDPOINT: Takes advantage of the new database structure
     */
    @GetMapping("/unread")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<List<NotificationMessage>> getUnreadNotifications() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUserEmail = authentication.getName();
        
        // You'd need to add this method to NotificationService
        List<NotificationMessage> unreadNotifications = notificationService.getUnreadNotificationsByEmail(currentUserEmail);
        return ResponseEntity.ok(unreadNotifications);
    }
    
    /**
     * GET /api/notifications/count/unread
     * Get count of unread notifications
     * NEW ENDPOINT: Useful for notification badges
     */
    @GetMapping("/count/unread")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Integer> getUnreadNotificationCount() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUserEmail = authentication.getName();
        
        // You'd need to add this method to NotificationService
        int unreadCount = notificationService.getUnreadNotificationCount(currentUserEmail);
        return ResponseEntity.ok(unreadCount);
    }








     /**
     * POST /api/notifications/test-notification
     * Create a test notification for the current user
     */
    @PostMapping("/test-notification")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<MessageResponse> createTestNotification() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUserEmail = authentication.getName();
        
        notificationService.addNotification(
            currentUserEmail,
            "Test Notification",
            "This is a test notification to verify the API is working correctly.",
            NotificationConstants.TYPE_SYSTEM
        );
        
        return ResponseEntity.ok(new MessageResponse("Test notification created"));
    }

    // Add to your NotificationController for testing
@PostMapping("/test-booking-notifications")
@PreAuthorize("hasRole('USER')")
public ResponseEntity<MessageResponse> testBookingNotifications() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    String currentUserEmail = authentication.getName();
    
    // Test check-in warning notification
    notificationService.addNotification(
        currentUserEmail,
        "Check-in Required Soon",
        "Your booking for seat A1 started at 10:00. Please check in within the next 10 minutes or your booking will be automatically cancelled.",
        NotificationConstants.TYPE_CHECK_IN_WARNING
    );
    
    // Test no-show notification
    notificationService.addNotification(
        currentUserEmail,
        "Booking Cancelled - No Show",
        "Your booking for seat A1 scheduled at 10:00 has been cancelled because you did not check in within 20 minutes of the start time.",
        NotificationConstants.TYPE_NO_SHOW
    );
    
    // Test waitlist notification
    notificationService.addNotification(
        currentUserEmail,
        "Seat Available - Wait List",
        "Good news! The seat you were waiting for is now available: Seat A1 from 2024-01-20 10:00 to 2024-01-20 12:00",
        NotificationConstants.TYPE_WAITLIST
    );
    
    return ResponseEntity.ok(new MessageResponse("Test booking notifications created"));
}
}