package com.auca.library.dto.response;

import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class ProfessorDashboardResponse {
    // Professor info
    private String fullName;
    private boolean professorApproved;
    private int approvedCourseCount;
    
    // Quick stats
    private int totalRequests;
    private int pendingRequests;
    private int approvedRequests;
    private int activeBookings; // Currently using equipment
    
    // Recent requests (last 5)
    private List<EquipmentRequestResponse> recentRequests;
    
    // Upcoming scheduled equipment usage
    private List<UpcomingEquipmentUsage> upcomingUsage;
    
    // Available resources count
    private int availableEquipmentCount;
    private int availableLabClassCount;
    
    // Notifications/alerts
    private boolean hasRejectedRequests; // Has requests that can be escalated
    private int escalatableRequestCount;
    private boolean hasExpiringBookings; // Bookings ending in next 24 hours
    
    @Getter
    @Setter
    public static class UpcomingEquipmentUsage {
        private Long requestId;
        private String equipmentName;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private String courseName;
        private String labClassName; // If lab class booking
        private int hoursUntilStart;
    }
}