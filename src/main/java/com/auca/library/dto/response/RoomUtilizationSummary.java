package com.auca.library.dto.response;

import lombok.Data;

@Data
public class RoomUtilizationSummary {
    private RoomResponse room;
    private Integer totalBookings;
    private Double totalHours;
    private Double utilizationPercentage;
    private Double averageBookingDuration;
}