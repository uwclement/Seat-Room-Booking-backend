package com.auca.library.dto.response;

import lombok.Data;

@Data
public class QuickStatsResponse {
    private int todayBookings;
    private int todayActiveBookings;
    private int pendingApprovals;
    private int currentOccupancy;
    private int weeklyBookings;
    private int totalRooms;
    private int availableRooms;
    private double weeklyUtilizationRate;
    private int overdueApprovals;
    private int noShowBookings;
}