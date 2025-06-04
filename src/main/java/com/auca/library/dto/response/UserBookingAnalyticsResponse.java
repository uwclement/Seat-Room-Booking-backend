package com.auca.library.dto.response;

import lombok.Data;

@Data
public class UserBookingAnalyticsResponse {
    private UserResponse user;
    private int totalBookings;
    private int confirmedBookings;
    private int cancelledBookings;
    private double averageBookingDuration;
    private String mostUsedRoom;
    private int noShowCount;
    private double cancellationRate;
}