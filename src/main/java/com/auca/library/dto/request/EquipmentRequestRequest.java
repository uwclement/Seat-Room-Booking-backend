package com.auca.library.dto.request;

import java.time.LocalDateTime;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

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

    // For suggestion responses
    private Boolean suggestionAcknowledged; // true = acknowledged, false = rejected
    private String suggestionResponseReason; // optional reason for rejection
    
    // For extension requests
    @DecimalMin(value = "0.1", message = "Extension must be at least 0.1 hours")
    @DecimalMax(value = "3.0", message = "Extension cannot exceed 3 hours")
    private Double extensionHoursRequested;
    private String extensionReason;
    
    // For return process (Equipment Admin only)
    private String returnCondition; // GOOD, DAMAGED, MISSING_PARTS, LOST
    private String returnNotes;
    
    // For approval/rejection responses
    private String rejectionReason;
    private String adminSuggestion;
    
    // Reusable approval field
    private Boolean approved;
}