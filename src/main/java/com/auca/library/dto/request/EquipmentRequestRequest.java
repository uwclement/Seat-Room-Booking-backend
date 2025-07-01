package com.auca.library.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
public class EquipmentRequestRequest {
    @NotNull(message = "Equipment ID is required")
    private Long equipmentId;
    
    private Long courseId; // Required for professors
    
    private Long labClassId; // Optional
    
    @NotNull(message = "Reason is required")
    private String reason;
    
    @NotNull(message = "Start time is required")
    private LocalDateTime startTime;
    
    @NotNull(message = "End time is required")
    private LocalDateTime endTime;
    
    @Positive(message = "Requested quantity must be positive")
    private Integer requestedQuantity = 1;
}