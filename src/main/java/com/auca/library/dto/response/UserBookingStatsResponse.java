package com.auca.library.dto.response;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
public class UserBookingStatsResponse {
    private Integer totalBookings;
    private Integer completedBookings;
    private Integer cancelledBookings;
    private Integer noShowBookings;
    private Integer upcomingBookings;
    
    // Usage patterns
    private Double averageBookingDuration; // in hours
    private Integer totalHoursBooked;
    private Double utilizationRate; // percentage of bookings actually used
    
    // Preferences
    private List<RoomUsageStats> mostUsedRooms;
    private List<String> preferredTimeSlots;
    private Map<String, Integer> bookingsByDayOfWeek;
    private Map<String, Integer> bookingsByHour;
    
    // Recent activity
    private LocalDateTime lastBookingDate;
    private LocalDateTime nextUpcomingBooking;
    private Integer streakDays; // consecutive days with bookings
    
    // Performance metrics
    private Double punctualityScore; // percentage of on-time check-ins
    private Double reliabilityScore; // percentage of non-cancelled bookings
    
    @Data
    public static class RoomUsageStats {
        private RoomResponse room;
        private Integer bookingCount;
        private Double totalHours;
        private LocalDateTime lastUsed;
    }
}