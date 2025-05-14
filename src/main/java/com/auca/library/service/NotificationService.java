package com.auca.library.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.auca.library.dto.request.NotificationMessage;
import com.auca.library.exception.ResourceNotFoundException;
import com.auca.library.model.Seat;
import com.auca.library.model.User;
import com.auca.library.repository.UserRepository;
import com.auca.library.util.NotificationConstants;

/**
 * Service for managing user notifications.
 * Handles creation, reading, marking notifications as read,
 * and automatic cleanup of expired notifications.
 */
@Service
public class NotificationService {
    
    // Maximum number of notifications to keep per user
    private static final int MAX_NOTIFICATIONS_PER_USER = 50;
    
    @Autowired
    private UserRepository userRepository;
    
    /**
     * Create and add a notification with custom expiration time
     * 
     * @param userEmail Email of the user to notify
     * @param title Notification title
     * @param message Notification message content
     * @param type Notification type (used for categorization and default expiration)
     * @param expirationHours Number of hours until notification expires
     */
    @Transactional
    public void addNotification(String userEmail, String title, String message, 
                              String type, Long expirationHours) {
        User user = findUserByEmail(userEmail);
        
        // Create new notification with specified expiration
        NotificationMessage notification = new NotificationMessage(title, message, type, expirationHours);
        
        // Add notification to user's list
        addNotificationToUser(user, notification);
    }
    
    /**
     * Create and add a notification with default expiration time for its type
     * 
     * @param userEmail Email of the user to notify
     * @param title Notification title
     * @param message Notification message content
     * @param type Notification type (determines default expiration)
     */
    @Transactional
    public void addNotification(String userEmail, String title, String message, String type) {
        long expirationHours = NotificationConstants.getDefaultExpirationHours(type);
        addNotification(userEmail, title, message, type, expirationHours);
    }
    
    /**
     * Get all notifications for the currently authenticated user
     * Automatically removes expired notifications during retrieval
     * 
     * @return List of active notifications for current user
     */
    public List<NotificationMessage> getCurrentUserNotifications() {
        String currentUserEmail = getCurrentUserEmail();
        User user = findUserByEmail(currentUserEmail);
        
        List<NotificationMessage> notifications = user.getNotifications();
        
        // Clean up expired notifications when reading (opportunistic cleanup)
        if (!notifications.isEmpty()) {
            int originalSize = notifications.size();
            removeExpiredNotifications(notifications);
            
            // Save only if cleanup actually removed notifications
            if (notifications.size() < originalSize) {
                saveNotificationsToUser(user, notifications);
            }
        }
        
        return notifications;
    }
    
    /**
     * Mark a specific notification as read
     * 
     * @param notificationId ID of the notification to mark as read
     */
    @Transactional
    public void markAsRead(String notificationId) {
        String currentUserEmail = getCurrentUserEmail();
        User user = findUserByEmail(currentUserEmail);
        
        List<NotificationMessage> notifications = user.getNotifications();
        boolean notificationFound = false;
        
        // Find and mark the specific notification as read
        for (NotificationMessage notification : notifications) {
            if (notification.getId().equals(notificationId)) {
                notification.setRead(true);
                notificationFound = true;
                break;
            }
        }
        
        // Save only if we actually updated a notification
        if (notificationFound) {
            saveNotificationsToUser(user, notifications);
        }
    }
    
    /**
     * Mark all unread notifications as read for current user
     */
    @Transactional
    public void markAllAsRead() {
        String currentUserEmail = getCurrentUserEmail();
        User user = findUserByEmail(currentUserEmail);
        
        List<NotificationMessage> notifications = user.getNotifications();
        boolean hasUnreadNotifications = false;
        
        // Mark all unread notifications as read
        for (NotificationMessage notification : notifications) {
            if (!notification.isRead()) {
                notification.setRead(true);
                hasUnreadNotifications = true;
            }
        }
        
        // Save only if we actually updated notifications
        if (hasUnreadNotifications) {
            saveNotificationsToUser(user, notifications);
        }
    }
    
    /**
     * Send notification when a waitlisted seat becomes available
     * 
     * @param user User who was waiting for the seat
     * @param seat The available seat
     * @param startTime Booking start time
     * @param endTime Booking end time
     */
    public void sendWaitListNotification(User user, Seat seat, 
                                        LocalDateTime startTime, LocalDateTime endTime) {
        // Format the notification message with seat details
        String message = String.format(
            "Good news! The seat you were waiting for is now available: Seat %s from %s to %s",
            seat.getSeatNumber(),
            startTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")),
            endTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
        );
        
        NotificationMessage notification = new NotificationMessage(
            "Seat Available - Wait List",
            message,
            NotificationConstants.TYPE_WAITLIST,
            NotificationConstants.getDefaultExpirationHours(NotificationConstants.TYPE_WAITLIST)
        );
        
        // Add seat booking information to metadata for future reference
        notification.getMetadata().put("seatId", seat.getId());
        notification.getMetadata().put("seatNumber", seat.getSeatNumber());
        notification.getMetadata().put("startTime", startTime.toString());
        notification.getMetadata().put("endTime", endTime.toString());
        
        addNotificationToUser(user, notification);
    }
    
    /**
     * Send notification when a user doesn't show up for their booking
     * 
     * @param user User who missed their booking
     * @param seatNumber Seat that was booked
     * @param startTime Original booking start time
     */
    public void sendNoShowNotification(User user, String seatNumber, LocalDateTime startTime) {
        String message = String.format(
            "Your booking for seat %s scheduled at %s has been cancelled because you did not check in within 20 minutes of the start time.",
            seatNumber,
            startTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
        );
        
        addNotification(
            user.getEmail(),
            "Booking Cancelled - No Show",
            message,
            NotificationConstants.TYPE_NO_SHOW
        );
    }
    
    /**
     * Send library-wide information notification to all active users
     * Used for announcements, system updates, etc.
     * 
     * @param title Notification title
     * @param message Notification message
     */
    @Transactional
    public void sendLibraryInfoNotification(String title, String message) {
        List<User> activeUsers = userRepository.findActiveUsers();
        
        // Send notification to each active user
        for (User user : activeUsers) {
            addNotification(
                user.getEmail(),
                title,
                message,
                NotificationConstants.TYPE_LIBRARY_INFO
            );
        }
    }
    
    /**
     * Scheduled task to clean up expired notifications for all users
     * Runs every hour to maintain database performance
     */
    @Scheduled(cron = "0 0 * * * *") // Execute at minute 0 of every hour
    @Transactional
    public void cleanupAllExpiredNotifications() {
        List<User> usersWithNotifications = userRepository.findAllWithNotifications();
        
        // Process each user's notifications
        for (User user : usersWithNotifications) {
            List<NotificationMessage> notifications = user.getNotifications();
            int originalSize = notifications.size();
            
            // Remove expired notifications
            removeExpiredNotifications(notifications);
            
            // Save only if cleanup actually removed notifications
            if (notifications.size() < originalSize) {
                saveNotificationsToUser(user, notifications);
            }
        }
    }
    
    // === Private Helper Methods ===
    
    /**
     * Add notification to user's list and save to database
     * Handles the common logic for adding notifications
     */
    private void addNotificationToUser(User user, NotificationMessage notification) {
        List<NotificationMessage> notifications = user.getNotifications();
        
        // Add new notification at the beginning of the list (most recent first)
        notifications.add(0, notification);
        
        // Clean up expired notifications to maintain performance
        removeExpiredNotifications(notifications);
        
        // Save the updated notifications
        saveNotificationsToUser(user, notifications);
    }
    
    /**
     * Save notification list to user and persist to database
     * Encapsulates the saving logic for consistency
     */
    private void saveNotificationsToUser(User user, List<NotificationMessage> notifications) {
        user.setNotificationsList(notifications);
        // Note: user.saveNotifications() is deprecated when using JPA converter
        // The converter automatically handles JSON serialization when saving
        userRepository.save(user);
    }
    
    /**
     * Remove expired notifications and limit total count
     * Helps maintain performance by preventing notification buildup
     */
    private void removeExpiredNotifications(List<NotificationMessage> notifications) {
        // Remove notifications that have passed their expiration time
        notifications.removeIf(NotificationMessage::isExpired);
        
        // Limit the total number of notifications per user to maintain performance
        if (notifications.size() > MAX_NOTIFICATIONS_PER_USER) {
            // Keep only the most recent notifications
            List<NotificationMessage> limited = new ArrayList<>(
                notifications.subList(0, MAX_NOTIFICATIONS_PER_USER)
            );
            notifications.clear();
            notifications.addAll(limited);
        }
    }
    
    /**
     * Find user by email with proper error handling
     */
    private User findUserByEmail(String email) {
        return userRepository.findByEmail(email)
            .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
    }
    
    /**
     * Get the email of the currently authenticated user
     */
    private String getCurrentUserEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication.getName();
    }
}