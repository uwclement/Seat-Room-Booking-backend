package com.auca.library.dto.response;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class NextAvailableSlotResponse {
    private Long roomId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer durationHours;
    private String message;
}