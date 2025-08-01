package com.auca.library.service;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.auca.library.dto.request.NotificationMessage;
import com.auca.library.exception.ResourceNotFoundException;
import com.auca.library.model.Notification;
import com.auca.library.model.Seat;
import com.auca.library.model.User;
import com.auca.library.repository.NotificationRepository;
import com.auca.library.repository.UserRepository;
import com.auca.library.util.NotificationConstants;

@Service
public class NotificationService {

    private static final int MAX_NOTIFICATIONS_PER_USER = 50;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private UserRepository userRepository;

    // For SSE connections - maps userEmail to list of SSE connections
    private final Map<String, List<SseEmitter>> userSubscribers = new ConcurrentHashMap<>();

    /**
     * Create and add a notification with custom expiration time
     */
    @Transactional
    public void addNotification(String userEmail, String title, String message,
            String type, Long expirationHours) {
        User user = findUserByEmail(userEmail);

        // Calculate expiration time
        LocalDateTime expiresAt = LocalDateTime.now().plusHours(expirationHours);

        // Create new notification entity
        Notification notification = new Notification(user, title, message, type, expiresAt);

        // Save to database
        notification = notificationRepository.save(notification);

        // Broadcast to connected SSE clients
        broadcastNotificationToUser(userEmail, convertToNotificationMessage(notification));

        // Maintain max notifications per user
        maintainMaxNotificationsPerUser(user);
    }

    /**
     * Create and add a notification with default expiration time for its type
     */
    @Transactional
    public void addNotification(String userEmail, String title, String message, String type) {
        long expirationHours = NotificationConstants.getDefaultExpirationHours(type);
        addNotification(userEmail, title, message, type, expirationHours);
    }

    /**
     * Get all notifications for the currently authenticated user
     */
    public List<NotificationMessage> getCurrentUserNotifications() {
        String currentUserEmail = getCurrentUserEmail();
        List<Notification> notifications = notificationRepository.findByUserEmailOrderByCreatedAtDesc(currentUserEmail);

        return notifications.stream()
                .map(this::convertToNotificationMessage)
                .collect(Collectors.toList());
    }

    /**
     * Get unread notifications for a specific user
     */
    public List<NotificationMessage> getUnreadNotifications(Long userId) {
        List<Notification> notifications = notificationRepository.findUnreadByUserId(userId);

        return notifications.stream()
                .map(this::convertToNotificationMessage)
                .collect(Collectors.toList());
    }

    /**
     * Get unread notifications by email
     */
    public List<NotificationMessage> getUnreadNotificationsByEmail(String email) {
        User user = findUserByEmail(email);
        return getUnreadNotifications(user.getId());
    }

    /**
     * Get count of unread notifications
     */
    public int getUnreadNotificationCount(String email) {
        User user = findUserByEmail(email);
        List<Notification> unread = notificationRepository.findUnreadByUserId(user.getId());
        return unread.size();
    }

    /**
     * Mark a specific notification as read
     */
    @Transactional
    public void markAsRead(Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found with id: " + notificationId));

        notification.setRead(true);
        notificationRepository.save(notification);
    }

    /**
     * Mark all unread notifications as read for current user
     */
    @Transactional
    public void markAllAsRead() {
        String currentUserEmail = getCurrentUserEmail();
        User user = findUserByEmail(currentUserEmail);

        List<Notification> unreadNotifications = notificationRepository.findUnreadByUserId(user.getId());

        unreadNotifications.forEach(notification -> notification.setRead(true));
        notificationRepository.saveAll(unreadNotifications);
    }

    /**
     * Send notification when a waitlisted seat becomes available
     */
    public void sendWaitListNotification(User user, Seat seat,
            LocalDateTime startTime, LocalDateTime endTime) {
        // Format the notification message with seat details
        String message = String.format(
                "Good news! The seat you were waiting for is now available: Seat %s from %s to %s",
                seat.getSeatNumber(),
                startTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")),
                endTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));

        addNotification(
                user.getEmail(),
                "Seat Available - Wait List",
                message,
                NotificationConstants.TYPE_WAITLIST);
    }

    
     // Send notification when a user doesn't show up for their booking
     
    public void sendNoShowNotification(User user, String seatNumber, LocalDateTime startTime) {
        String message = String.format(
                "Your booking for seat %s scheduled at %s has been cancelled because you did not check in within 20 minutes of the start time.",
                seatNumber,
                startTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));

        addNotification(
                user.getEmail(),
                "Booking Cancelled - No Show",
                message,
                NotificationConstants.TYPE_NO_SHOW);
    }

    // check-in confirmation 
    public void sendCheckInConfirmation(User user, String seatNumber, LocalDateTime startTime, boolean isAdminAction) {
    String message;
    
    if (isAdminAction) {
        message = String.format(
            "You have been checked in by an administrator for seat %s scheduled at %s. " +
            "Have a productive session!",
            seatNumber,
            startTime.format(DateTimeFormatter.ofPattern("MMM dd, yyyy 'at' h:mm a"))
        );
    } else {
        message = String.format(
            "You have successfully checked in to seat %s scheduled at %s. " +
            "Enjoy your study session!",
            seatNumber,
            startTime.format(DateTimeFormatter.ofPattern("MMM dd, yyyy 'at' h:mm a"))
        );
    }

    addNotification(
        user.getEmail(),
        "Booking Check-In Confirmed",
        message,
        NotificationConstants.TYPE_LIBRARY_INFO
    );
}

    public void sendBookingCancellationNotification(User user, String seatNumber, LocalDateTime startTime, 
                                               String cancellationReason, boolean isAdminAction) {
    String message;
    String title;
    
    if (isAdminAction) {
        title = "Booking Cancelled by Administrator";
        message = String.format(
            "Your booking for seat %s scheduled on %s has been cancelled by an administrator.\n\n" +
            "Reason: %s\n\n" +
            "If you have any questions, please contact the library administration. " +
            "You can make a new booking through the system.",
            seatNumber,
            startTime.format(DateTimeFormatter.ofPattern("MMM dd, yyyy 'at' h:mm a")),
            cancellationReason != null && !cancellationReason.trim().isEmpty() 
                ? cancellationReason 
                : "Administrative action required"
        );
    } else {
        title = "Booking Cancelled";
        message = String.format(
            "Your booking for seat %s scheduled on %s has been cancelled.\n\n" +
            "Reason: %s\n\n" +
            "You can make a new booking through the system.",
            seatNumber,
            startTime.format(DateTimeFormatter.ofPattern("MMM dd, yyyy 'at' h:mm a")),
            cancellationReason != null && !cancellationReason.trim().isEmpty() 
                ? cancellationReason 
                : "Booking cancelled"
        );
    }

    addNotification(
        user.getEmail(),
        title,
        message,
        NotificationConstants.TYPE_LIBRARY_INFO
    );
}



    
     // Send library-wide information notification to all active users
     
    @Transactional
    public void sendLibraryInfoNotification(String title, String message) {
        List<User> activeUsers = userRepository.findActiveUsers();

        // Send notification to each active user
        for (User user : activeUsers) {
            addNotification(
                    user.getEmail(),
                    title,
                    message,
                    NotificationConstants.TYPE_LIBRARY_INFO);
        }
    }

    /**
     * Weekly cleanup task to remove notifications older than 1 week
     * Runs every Sunday at midnight
     */
    @Scheduled(cron = "0 0 0 * * SUN")
    @Transactional
    public void weeklyNotificationCleanup() {
        LocalDateTime oneWeekAgo = LocalDateTime.now().minusWeeks(1);
        notificationRepository.deleteOlderThan(oneWeekAgo);
    }

    // === SSE Support Methods ===

    /**
     * Add an SSE subscriber for a user
     */
    public void addNotificationSubscriber(String userEmail, SseEmitter emitter) {
        userSubscribers.computeIfAbsent(userEmail, k -> new CopyOnWriteArrayList<>()).add(emitter);
    }

    /**
     * Remove an SSE subscriber for a user
     */
    public void removeNotificationSubscriber(String userEmail, SseEmitter emitter) {
        List<SseEmitter> emitters = userSubscribers.get(userEmail);
        if (emitters != null) {
            emitters.remove(emitter);
            if (emitters.isEmpty()) {
                userSubscribers.remove(userEmail);
            }
        }
    }

    /**
     * Broadcast notification to all connected SSE clients of a user
     */
    private void broadcastNotificationToUser(String userEmail, NotificationMessage notificationMessage) {
        List<SseEmitter> emitters = userSubscribers.get(userEmail);
        if (emitters != null) {
            List<SseEmitter> deadEmitters = new ArrayList<>();

            for (SseEmitter emitter : emitters) {
                try {
                    emitter.send(notificationMessage);
                } catch (IOException e) {
                    deadEmitters.add(emitter);
                }
            }

            // Remove dead connections
            emitters.removeAll(deadEmitters);

            // Remove user from map if no connections left
            if (emitters.isEmpty()) {
                userSubscribers.remove(userEmail);
            }
        }
    }

    // === Private Helper Methods ===

    /**
     * Convert Notification entity to NotificationMessage DTO
     * Sets empty metadata since we no longer store it in the entity
     */
    private NotificationMessage convertToNotificationMessage(Notification notification) {
        NotificationMessage message = new NotificationMessage();
        message.setId(notification.getId().toString());
        message.setTitle(notification.getTitle());
        message.setMessage(notification.getMessage());
        message.setType(notification.getType());
        message.setTimestamp(notification.getCreatedAt().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
        message.setExpirationTime(
                notification.getExpiresAt().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
        message.setRead(notification.isRead());
        message.setMetadata(new HashMap<>()); // Empty metadata
        return message;
    }

    /**
     * Maintain maximum number of notifications per user
     * Delete oldest notifications when exceeding limit
     */
    private void maintainMaxNotificationsPerUser(User user) {
        List<Notification> userNotifications = notificationRepository
                .findByUserEmailOrderByCreatedAtDesc(user.getEmail());

        if (userNotifications.size() > MAX_NOTIFICATIONS_PER_USER) {
            // Get notifications to delete (oldest ones)
            List<Notification> notificationsToDelete = userNotifications.subList(
                    MAX_NOTIFICATIONS_PER_USER,
                    userNotifications.size());

            // Delete excess notifications in batch
            notificationsToDelete.forEach(notificationRepository::delete);
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

    // Send notification when library is about to close
    public void sendLibraryClosingNotification(User user, int minutesUntilClose) {
        String message = String.format(
                "The library will be closing in %d minutes. Please prepare to check out.",
                minutesUntilClose);

        addNotification(
                user.getEmail(),
                "Library Closing Soon",
                message,
                NotificationConstants.TYPE_LIBRARY_INFO);
    }

    // Send notification about schedule changes
    public void sendScheduleChangeNotification(String title, String message, LocalDate affectedDate) {
        List<User> activeUsers = userRepository.findActiveUsers();

        // Send notification to each active user
        for (User user : activeUsers) {
            addNotification(
                    user.getEmail(),
                    title,
                    message,
                    NotificationConstants.TYPE_SYSTEM);
        }
    }

    // // Scheduled task to check and send closing notifications
    // @Scheduled(fixedRate = 60000) // Check every minute
    // public void checkLibraryClosingTime() {
    // LibraryStatusResponse status =
    // libraryScheduleService.getCurrentLibraryStatus();

    // if (status.isOpen() && status.getNextStatusChange() != null) {
    // LocalDateTime now = LocalDateTime.now();
    // Duration timeUntilClose = Duration.between(now,
    // status.getNextStatusChange());

    // // Send notification 15 minutes before closing
    // if (timeUntilClose.toMinutes() == 15) {
    // sendClosingNotificationToAllUsers(15);
    // }
    // // Send notification 5 minutes before closing
    // else if (timeUntilClose.toMinutes() == 5) {
    // sendClosingNotificationToAllUsers(5);
    // }
    // }
    // }

    private void sendClosingNotificationToAllUsers(int minutesUntilClose) {
        List<User> activeUsers = userRepository.findActiveUsers();

        for (User user : activeUsers) {
            sendLibraryClosingNotification(user, minutesUntilClose);
        }
    }


    public void sendEquipmentRequestNotification(String userEmail, String equipmentName, String status, String reason) {
    String title = "Equipment Request " + status;
    String message = String.format("Your request for %s has been %s", equipmentName, status.toLowerCase());
    
    if (reason != null && !reason.trim().isEmpty()) {
        message += ". Reason: " + reason;
    }
    
    addNotification(userEmail, title, message, "EQUIPMENT_" + status.toUpperCase());
}

public void sendProfessorApprovalNotification(String professorEmail, boolean approved, String reason) {
    String title = approved ? "Professor Account Approved" : "Professor Account Rejected";
    String message = approved ? 
        "Your professor account has been approved. You can now select courses and make equipment requests." :
        "Your professor account application has been rejected.";
    
    if (!approved && reason != null) {
        message += " Reason: " + reason;
    }
    
    addNotification(professorEmail, title, message, 
        approved ? "PROFESSOR_APPROVED" : "PROFESSOR_REJECTED");
}

public void sendCourseApprovalNotification(String professorEmail, List<String> courseNames, boolean approved) {
    String title = approved ? "Courses Approved" : "Course Request Rejected";
    String coursesText = String.join(", ", courseNames);
    String message = approved ?
        String.format("Your courses have been approved: %s", coursesText) :
        String.format("Your course request has been rejected: %s", coursesText);
    
    addNotification(professorEmail, title, message, 
        approved ? "PROFESSOR_COURSES_APPROVED" : "PROFESSOR_COURSES_REJECTED");
}

public void sendLowInventoryAlert(String adminEmail, String equipmentName, int currentQuantity) {
    String title = "Low Inventory Alert";
    String message = String.format("Equipment '%s' has low inventory. Current available quantity: %d", 
        equipmentName, currentQuantity);
    
    addNotification(adminEmail, title, message, "LOW_INVENTORY_ALERT");
}

public void sendEscalationNotification(String hodEmail, String professorName, String equipmentName) {
    String title = "Equipment Request Escalation";
    String message = String.format("Professor %s has escalated a rejected equipment request for %s", 
        professorName, equipmentName);
    
    addNotification(hodEmail, title, message, "EQUIPMENT_ESCALATION");
}


// extend booking notification 

public void sendExtensionReminderNotification(User user, String seatNumber, LocalDateTime endTime) {
    String message = String.format(
        "Your booking for seat %s is ending at %s. You can extend it from your bookings page.",
        seatNumber,
        endTime.format(DateTimeFormatter.ofPattern("h:mm a"))
    );

    addNotification(
        user.getEmail(),
        "Booking Ending Soon",
        message,
        NotificationConstants.TYPE_LIBRARY_INFO
    );
}

}