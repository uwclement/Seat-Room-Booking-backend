package com.auca.library.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.auca.library.model.EquipmentUnit;
import com.auca.library.model.Location;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EquipmentUnitResponse {
    private Long id;
    private String serialNumber;
    private EquipmentUnit.UnitStatus status;
    private String condition;
    private Long equipmentId;
    private String equipmentName;
    private Location location;
    private LocalDate purchaseDate;
    private LocalDate warrantyExpiry;
    private String notes;
    private LocalDateTime createdAt;
    
    // Assignment info
    private boolean assigned = false;
    private String assignedTo;
    private String assignmentType;
}