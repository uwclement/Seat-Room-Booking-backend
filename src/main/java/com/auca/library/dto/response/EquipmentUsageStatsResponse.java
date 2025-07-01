package com.auca.library.dto.response;

import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
public class EquipmentUsageStatsResponse {
    private Long equipmentId;
    private String equipmentName;
    
    // Usage statistics
    private int totalRequests;
    private int approvedRequests;
    private int rejectedRequests;
    private int pendingRequests;
    
    // Time-based stats
    private int requestsThisMonth;
    private int requestsThisWeek;
    private LocalDateTime lastRequestDate;
    private LocalDateTime lastApprovalDate;
    
    // User statistics
    private int uniqueUsers;
    private int studentRequests;
    private int professorRequests;
    
    // Availability stats
    private double averageUtilizationRate; // Percentage of time equipment is in use
    private int totalHoursRequested;
    private int totalHoursApproved;
}