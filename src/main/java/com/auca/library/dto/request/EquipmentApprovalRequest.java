package com.auca.library.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EquipmentApprovalRequest {
    @NotNull
    private Long bookingId;
    
    @NotNull
    private Long equipmentId;
    
    @NotNull
    private Boolean approved;
    
    private String reason; 
}