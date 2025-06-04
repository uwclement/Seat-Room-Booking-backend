package com.auca.library.dto.response;

import java.time.LocalDateTime;
import java.util.List;

import lombok.Data;

public class AnalyticsResponse {
    private RoomUsageAnalytics roomUsage;
    private UserBehaviorAnalytics userBehavior;
    private SystemStatistics systemStats;
    private List<PeakTimeData> peakTimes;
    
    @Data
    public static class RoomUsageAnalytics {
        private List<RoomUsageData> roomData;
        private List<RoomResponse> mostUsedRooms;
        private List<RoomResponse> underutilizedRooms;
        private Double averageUtilization;
    }
    
    @Data
    public static class RoomUsageData {
        private RoomResponse room;
        private Integer totalBookings;
        private Double averageBookingDuration;
        private Double averageActualUsage;
        private Integer noShowCount;
        private Double utilizationRate;
    }
    
    @Data
    public static class UserBehaviorAnalytics {
        private Integer totalActiveUsers;
        private Double averageBookingsPerUser;
        private Double systemNoShowRate;
        private List<UserBehaviorData> topUsers;
    }
    
    @Data
    public static class UserBehaviorData {
        private UserResponse user;
        private Integer totalBookings;
        private Double averageUsageDuration;
        private Integer noShowCount;
        private Double noShowRate;
    }
    
    @Data
    public static class SystemStatistics {
        private Integer totalBookings;
        private Integer activeBookings;
        private Integer completedBookings;
        private Integer cancelledBookings;
        private Double overallUtilization;
        private LocalDateTime reportPeriodStart;
        private LocalDateTime reportPeriodEnd;
    }
    
    @Data
    public static class PeakTimeData {
        private Integer hour;
        private String dayOfWeek;
        private Integer bookingCount;
        private Double utilizationPercentage;
    }
}
