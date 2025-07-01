package com.auca.library.dto.response;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class EquipmentAdminDashboardResponse {
    // Quick stats
    private int totalEquipment;
    private int availableEquipment;
    private int totalLabClasses;
    private int availableLabClasses;
    private int totalCourses;
    private int activeCourses;
    
    // Request statistics
    private int pendingRequests;
    private int todaysRequests;
    private int thisWeekRequests;
    private int thisMonthRequests;
    
    // Equipment utilization
    private double overallUtilizationRate;
    private List<EquipmentUsageStatsResponse> topRequestedEquipment;
    private List<EquipmentUsageStatsResponse> underutilizedEquipment;
    
    // Recent activity
    private List<EquipmentRequestResponse> recentRequests;
    private List<RecentEquipmentActivity> recentActivity;
    
    // Alerts
    private int lowInventoryAlerts; // Equipment with low available quantity
    private int overdueApprovals; // Requests pending for more than 24 hours
    private boolean hasHodEscalations; // Whether any requests have been escalated
    
    public static class RecentEquipmentActivity {
        private String activityType; // "REQUEST_CREATED", "REQUEST_APPROVED", etc.
        private String equipmentName;
        private String userName;
        private LocalDateTime timestamp;
        private String description;
    }
}