package com.auca.library.dto.response;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class RoomUtilizationDashboardResponse {
    private LocalDateTime periodStart;
    private LocalDateTime periodEnd;
    private Integer totalDays;
    private Integer totalRooms;
    private Integer totalBookings;
    private Integer currentlyOccupiedRooms;
    private Double currentOccupancyPercentage;
    private Double averageBookingDuration;
    private RoomResponse mostPopularRoom;
    private RoomResponse leastUsedRoom;
    private List<Integer> peakHours;
    private List<RoomUtilizationSummary> roomUtilizations;
    private Double overallUtilization;
}
