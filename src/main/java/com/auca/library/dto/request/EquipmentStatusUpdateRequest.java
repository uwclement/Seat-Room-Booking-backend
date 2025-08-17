package com.auca.library.dto.request;

import com.auca.library.model.EquipmentStatus;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EquipmentStatusUpdateRequest {
    @NotNull
    private EquipmentStatus fromStatus;
    
    @NotNull
    private EquipmentStatus toStatus;
    
    @NotNull
    @Min(1)
    private Integer quantity;
    
    private String notes;
}