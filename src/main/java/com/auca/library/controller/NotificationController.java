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
     */
    @PostMapping("/{id}/read")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<MessageResponse> markAsRead(@PathVariable String id) {
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

}