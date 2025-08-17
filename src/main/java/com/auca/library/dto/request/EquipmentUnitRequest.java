package com.auca.library.dto.request;

import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EquipmentUnitRequest {
    @NotNull
    private Long equipmentId;
    
    @NotNull
    private String serialNumber;
    
    private String condition = "GOOD";
    private LocalDate purchaseDate;
    private LocalDate warrantyExpiry;
    private String notes;
}