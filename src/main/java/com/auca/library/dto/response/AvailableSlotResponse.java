package com.auca.library.dto.response;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class AvailableSlotResponse {
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Long durationMinutes;
    private Double durationHours;
}