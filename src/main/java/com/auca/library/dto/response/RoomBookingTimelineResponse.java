package com.auca.library.dto.response;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class RoomBookingTimelineResponse {
    private RoomResponse room;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private Integer totalDays;
    private List<DailyBookingTimelineResponse> dailyTimelines;
    private Double overallUtilization;
}