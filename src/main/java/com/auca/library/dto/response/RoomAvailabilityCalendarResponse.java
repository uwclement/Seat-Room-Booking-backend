package com.auca.library.dto.response;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class RoomAvailabilityCalendarResponse {
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private Integer totalDays;
    private Integer totalRooms;
    private List<RoomCalendarResponse> roomCalendars;
    private Double overallUtilization;
}