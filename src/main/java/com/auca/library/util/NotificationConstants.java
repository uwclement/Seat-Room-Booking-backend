package com.auca.library.util;

import java.util.Map;

public class NotificationConstants {
    // Notification types
    public static final String TYPE_NO_SHOW = "NO_SHOW";
    public static final String TYPE_ROOM_SHARING = "ROOM_SHARING";
    public static final String TYPE_LIBRARY_INFO = "LIBRARY_INFO";
    public static final String TYPE_WAITLIST = "WAITLIST";
    public static final String TYPE_SYSTEM = "SYSTEM";
    

    // Get default expiration for a notification type
    public static long getDefaultExpirationHours(String type) {
        return DEFAULT_EXPIRATION_HOURS.getOrDefault(type, 24L); // Default to 24 hours
    }


    // Notification types
public static final String TYPE_CHECK_IN_WARNING = "CHECK_IN_WARNING";

// Add to DEFAULT_EXPIRATION_HOURS map
public static final Map<String, Long> DEFAULT_EXPIRATION_HOURS = Map.of(
    TYPE_NO_SHOW, 24L,              // 1 day
    TYPE_ROOM_SHARING, 48L,         // 2 days
    TYPE_LIBRARY_INFO, 72L,         // 3 days
    TYPE_WAITLIST, 24L,             // 1 day
    TYPE_SYSTEM, 168L,              // 1 week
    TYPE_CHECK_IN_WARNING, 2L       // 2 hours - Short expiration since it's time-sensitive
);
}