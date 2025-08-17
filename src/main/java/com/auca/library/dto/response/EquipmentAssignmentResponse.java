package com.auca.library.dto.response;

import java.time.LocalDateTime;

import com.auca.library.model.EquipmentAssignment;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EquipmentAssignmentResponse {
    private Long id;
    private Long equipmentUnitId;
    private String serialNumber;
    private String equipmentName;
    private EquipmentAssignment.AssignmentType assignmentType;
    private String assignedToName;
    private EquipmentAssignment.AssignmentPeriod assignmentPeriod;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private EquipmentAssignment.AssignmentStatus status;
    private String assignedBy;
    private LocalDateTime assignedAt;
    private String returnReason;
    private LocalDateTime returnedAt;
}