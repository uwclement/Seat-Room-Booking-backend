package com.auca.library.dto.response;

import java.time.LocalDateTime;

import com.auca.library.model.Location;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EquipmentSummaryResponse {
    private Location location;
    private Long totalUnits;
    private Long availableUnits;
    private Long assignedUnits;
    private Long maintenanceUnits;
    private Long damagedUnits;
    private Long staffAssignments;
    private Long roomAssignments;
    private LocalDateTime lastUpdated;
}