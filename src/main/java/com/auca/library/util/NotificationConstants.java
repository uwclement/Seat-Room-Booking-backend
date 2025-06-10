package com.auca.library.util;

import java.util.HashMap;
import java.util.Map;

public class NotificationConstants {
    public static final String TYPE_BOOKING_CONFIRMED = "BOOKING_CONFIRMED";
    public static final String TYPE_BOOKING_CANCELLED = "BOOKING_CANCELLED";
    public static final String TYPE_BOOKING_REMINDER = "BOOKING_REMINDER";
    public static final String TYPE_BOOKING_INVITATION = "BOOKING_INVITATION";
    public static final String TYPE_BOOKING_UPDATE = "BOOKING_UPDATE";
    public static final String TYPE_WAITLIST = "WAITLIST";
    public static final String TYPE_NO_SHOW = "NO_SHOW";
    public static final String TYPE_LIBRARY_INFO = "LIBRARY_INFO";
    public static final String TYPE_SYSTEM = "SYSTEM";

    public static final String TYPE_BOOKING_APPROVED = "BOOKING_APPROVED";
    public static final String TYPE_BOOKING_REJECTED = "BOOKING_REJECTED";
    public static final String TYPE_EQUIPMENT_APPROVED = "EQUIPMENT_APPROVED";
    public static final String TYPE_EQUIPMENT_REJECTED = "EQUIPMENT_REJECTED";
    public static final String TYPE_ADMIN_BOOKING_CANCELLED = "ADMIN_BOOKING_CANCELLED";
    public static final String TYPE_ADMIN_REMINDER = "ADMIN_REMINDER";
    public static final String TYPE_ADMIN_BROADCAST = "ADMIN_BROADCAST";
    public static final String TYPE_CAPACITY_WARNING = "CAPACITY_WARNING";
    public static final String TYPE_APPROVAL_REQUIRED = "APPROVAL_REQUIRED";
    
    // ========== DEFAULT EXPIRATION HOURS ==========
    private static final Map<String, Long> DEFAULT_EXPIRATION_HOURS = new HashMap<>();
    
    static {
        // Existing types
        DEFAULT_EXPIRATION_HOURS.put(TYPE_BOOKING_CONFIRMED, 24L);
        DEFAULT_EXPIRATION_HOURS.put(TYPE_BOOKING_CANCELLED, 48L);
        DEFAULT_EXPIRATION_HOURS.put(TYPE_BOOKING_REMINDER, 6L);
        DEFAULT_EXPIRATION_HOURS.put(TYPE_BOOKING_INVITATION, 72L);
        DEFAULT_EXPIRATION_HOURS.put(TYPE_BOOKING_UPDATE, 24L);
        DEFAULT_EXPIRATION_HOURS.put(TYPE_WAITLIST, 2L); // Short expiration for urgent notifications
        DEFAULT_EXPIRATION_HOURS.put(TYPE_NO_SHOW, 48L);
        DEFAULT_EXPIRATION_HOURS.put(TYPE_LIBRARY_INFO, 168L); // 1 week
        DEFAULT_EXPIRATION_HOURS.put(TYPE_SYSTEM, 72L);
        
        // New admin notification types
        DEFAULT_EXPIRATION_HOURS.put(TYPE_BOOKING_APPROVED, 48L);
        DEFAULT_EXPIRATION_HOURS.put(TYPE_BOOKING_REJECTED, 72L);
        DEFAULT_EXPIRATION_HOURS.put(TYPE_EQUIPMENT_APPROVED, 24L);
        DEFAULT_EXPIRATION_HOURS.put(TYPE_EQUIPMENT_REJECTED, 48L);
        DEFAULT_EXPIRATION_HOURS.put(TYPE_ADMIN_BOOKING_CANCELLED, 72L);
        DEFAULT_EXPIRATION_HOURS.put(TYPE_ADMIN_REMINDER, 24L);
        DEFAULT_EXPIRATION_HOURS.put(TYPE_ADMIN_BROADCAST, 168L); // 1 week for important broadcasts
        DEFAULT_EXPIRATION_HOURS.put(TYPE_CAPACITY_WARNING, 12L);
        DEFAULT_EXPIRATION_HOURS.put(TYPE_APPROVAL_REQUIRED, 72L);
    }
    
    /**
     * Get default expiration hours for a notification type
     */
    public static long getDefaultExpirationHours(String notificationType) {
        return DEFAULT_EXPIRATION_HOURS.getOrDefault(notificationType, 24L); // Default to 24 hours
    }
    
    // ========== NOTIFICATION PRIORITIES ==========
    public static final String PRIORITY_LOW = "LOW";
    public static final String PRIORITY_NORMAL = "NORMAL";
    public static final String PRIORITY_HIGH = "HIGH";
    public static final String PRIORITY_URGENT = "URGENT";
    
    /**
     * Get notification priority based on type
     */
    public static String getNotificationPriority(String notificationType) {
        switch (notificationType) {
            case TYPE_WAITLIST:
            case TYPE_ADMIN_BOOKING_CANCELLED:
            case TYPE_CAPACITY_WARNING:
                return PRIORITY_HIGH;
                
            case TYPE_NO_SHOW:
            case TYPE_BOOKING_REJECTED:
            case TYPE_EQUIPMENT_REJECTED:
            case TYPE_APPROVAL_REQUIRED:
                return PRIORITY_HIGH;
                
            case TYPE_BOOKING_APPROVED:
            case TYPE_BOOKING_CONFIRMED:
            case TYPE_EQUIPMENT_APPROVED:
            case TYPE_ADMIN_REMINDER:
                return PRIORITY_NORMAL;
                
            case TYPE_BOOKING_INVITATION:
            case TYPE_BOOKING_UPDATE:
            case TYPE_ADMIN_BROADCAST:
            case TYPE_LIBRARY_INFO:
                return PRIORITY_NORMAL;
                
            case TYPE_BOOKING_REMINDER:
            case TYPE_SYSTEM:
                return PRIORITY_LOW;
                
            default:
                return PRIORITY_NORMAL;
        }
    }
    
    // ========== NOTIFICATION CATEGORIES ==========
    public static final String CATEGORY_BOOKING = "BOOKING";
    public static final String CATEGORY_EQUIPMENT = "EQUIPMENT";
    public static final String CATEGORY_ADMIN = "ADMIN";
    public static final String CATEGORY_SYSTEM = "SYSTEM";
    public static final String CATEGORY_LIBRARY = "LIBRARY";
    
    /**
     * Get notification category based on type
     */
    public static String getNotificationCategory(String notificationType) {
        switch (notificationType) {
            case TYPE_BOOKING_CONFIRMED:
            case TYPE_BOOKING_CANCELLED:
            case TYPE_BOOKING_REMINDER:
            case TYPE_BOOKING_INVITATION:
            case TYPE_BOOKING_UPDATE:
            case TYPE_BOOKING_APPROVED:
            case TYPE_BOOKING_REJECTED:
            case TYPE_CAPACITY_WARNING:
            case TYPE_APPROVAL_REQUIRED:
                return CATEGORY_BOOKING;
                
            case TYPE_EQUIPMENT_APPROVED:
            case TYPE_EQUIPMENT_REJECTED:
                return CATEGORY_EQUIPMENT;
                
            case TYPE_ADMIN_BOOKING_CANCELLED:
            case TYPE_ADMIN_REMINDER:
            case TYPE_ADMIN_BROADCAST:
                return CATEGORY_ADMIN;
                
            case TYPE_LIBRARY_INFO:
                return CATEGORY_LIBRARY;
                
            case TYPE_WAITLIST:
            case TYPE_NO_SHOW:
            case TYPE_SYSTEM:
                return CATEGORY_SYSTEM;
                
            default:
                return CATEGORY_SYSTEM;
        }
    }
    
    // ========== MESSAGE TEMPLATES ==========
    
    /**
     * Get message template for booking approval notifications
     */
    public static String getBookingApprovalTemplate(String bookingTitle, boolean hasCapacityWarning, 
                                                   int confirmedParticipants, int roomCapacity) {
        String baseMessage = String.format("Your booking '%s' has been approved.", bookingTitle);
        
        if (hasCapacityWarning) {
            baseMessage += String.format(" Note: Room capacity is %d but only %d participants confirmed.", 
                                       roomCapacity, confirmedParticipants);
        }
        
        return baseMessage;
    }
    
    /**
     * Get message template for equipment approval notifications
     */
    public static String getEquipmentApprovalTemplate(String equipmentName, String bookingTitle, 
                                                     boolean approved, String reason) {
        if (approved) {
            return String.format("Your equipment request for '%s' in booking '%s' has been approved.", 
                               equipmentName, bookingTitle);
        } else {
            String message = String.format("Your equipment request for '%s' in booking '%s' has been rejected.", 
                                         equipmentName, bookingTitle);
            if (reason != null && !reason.trim().isEmpty()) {
                message += " Reason: " + reason;
            }
            return message;
        }
    }
    
    /**
     * Get message template for admin cancellation notifications
     */
    public static String getAdminCancellationTemplate(String bookingTitle, String reason, boolean isParticipant) {
        if (isParticipant) {
            return String.format("The booking '%s' you were participating in has been cancelled by an administrator. Reason: %s", 
                               bookingTitle, reason);
        } else {
            return String.format("Your booking '%s' has been cancelled by an administrator. Reason: %s", 
                               bookingTitle, reason);
        }
    }
    
    // ========== NOTIFICATION ACTIONS ==========
    public static final String ACTION_VIEW_BOOKING = "VIEW_BOOKING";
    public static final String ACTION_RESPOND_INVITATION = "RESPOND_INVITATION";
    public static final String ACTION_CHECK_IN = "CHECK_IN";
    public static final String ACTION_VIEW_EQUIPMENT = "VIEW_EQUIPMENT";
    public static final String ACTION_CONTACT_ADMIN = "CONTACT_ADMIN";
    public static final String ACTION_VIEW_ALTERNATIVE_ROOMS = "VIEW_ALTERNATIVE_ROOMS";
    
    /**
     * Get recommended action for notification type
     */
    public static String getRecommendedAction(String notificationType) {
        switch (notificationType) {
            case TYPE_BOOKING_INVITATION:
                return ACTION_RESPOND_INVITATION;
                
            case TYPE_BOOKING_CONFIRMED:
            case TYPE_BOOKING_APPROVED:
                return ACTION_CHECK_IN;
                
            case TYPE_BOOKING_REJECTED:
            case TYPE_ADMIN_BOOKING_CANCELLED:
                return ACTION_VIEW_ALTERNATIVE_ROOMS;
                
            case TYPE_EQUIPMENT_REJECTED:
                return ACTION_CONTACT_ADMIN;
                
            case TYPE_BOOKING_REMINDER:
                return ACTION_CHECK_IN;
                
            default:
                return ACTION_VIEW_BOOKING;
        }
    }


    // Notification types
public static final String TYPE_CHECK_IN_WARNING = "CHECK_IN_WARNING";

}