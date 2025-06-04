package com.auca.library.dto.response;

import lombok.Data;

@Data
public class RoomUtilizationAnalyticsResponse {
    private RoomResponse room;
    private int totalBookings;
    private double totalHours;
    private double utilizationPercentage;
    private double averageBookingDuration;
    private int uniqueUsers;
    private int peakHour;
}