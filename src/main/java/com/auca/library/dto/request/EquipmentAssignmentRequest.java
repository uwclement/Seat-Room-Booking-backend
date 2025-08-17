package com.auca.library.dto.request;

import java.time.LocalDateTime;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class EquipmentAssignmentRequest {
    
    @NotNull(message = "Equipment unit ID is required")
    private Long equipmentUnitId;
    
    @NotNull(message = "Assignment type is required")
    private String assignmentType; 
    
    @NotBlank(message = "Assigned to name is required")
    private String assignedToName; 
    
    @NotNull(message = "Assignment period is required")
    private String assignmentPeriod;
    
    private LocalDateTime startDate;
    private LocalDateTime endDate;

    // Constructors
    public EquipmentAssignmentRequest() {}

    // Getters and setters
    public Long getEquipmentUnitId() {
        return equipmentUnitId;
    }

    public void setEquipmentUnitId(Long equipmentUnitId) {
        this.equipmentUnitId = equipmentUnitId;
    }

    public String getAssignmentType() {
        return assignmentType;
    }

    public void setAssignmentType(String assignmentType) {
        this.assignmentType = assignmentType;
    }

    public String getAssignedToName() {
        return assignedToName;
    }

    public void setAssignedToName(String assignedToName) {
        this.assignedToName = assignedToName;
    }

    public String getAssignmentPeriod() {
        return assignmentPeriod;
    }

    public void setAssignmentPeriod(String assignmentPeriod) {
        this.assignmentPeriod = assignmentPeriod;
    }

    public LocalDateTime getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDateTime startDate) {
        this.startDate = startDate;
    }

    public LocalDateTime getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDateTime endDate) {
        this.endDate = endDate;
    }
}