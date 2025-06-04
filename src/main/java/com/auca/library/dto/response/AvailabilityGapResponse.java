package com.auca.library.dto.response;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class AvailabilityGapResponse {
    private Long roomId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer durationMinutes;
    private Double durationHours;
    private boolean bookable;
}