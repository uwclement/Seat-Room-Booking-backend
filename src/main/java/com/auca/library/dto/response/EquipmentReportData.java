package com.auca.library.dto.response;


import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EquipmentReportData {
    // Summary counts
    private Long totalEquipment;
    private Long availableEquipment;
    private Long assignedEquipment;
    private Long maintenanceEquipment;
    private Long damagedEquipment;
    private Long staffAssignmentCount;
    private Long roomAssignmentCount;
    
    // Detailed lists
    private java.util.List<com.auca.library.model.EquipmentUnit> allEquipmentUnits;
    private java.util.List<com.auca.library.model.EquipmentUnit> maintenanceUnits;
    private java.util.List<com.auca.library.model.EquipmentUnit> damagedUnits;
    private java.util.List<com.auca.library.model.EquipmentAssignment> activeAssignments;
    private java.util.Map<String, Long> mostRequestedEquipment;
}