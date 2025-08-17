package com.auca.library.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "equipment_assignments")
@Getter
@Setter
@NoArgsConstructor
public class EquipmentAssignment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "equipment_unit_id", nullable = false)
    private EquipmentUnit equipmentUnit;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AssignmentType assignmentType;

    @ManyToOne
    @JoinColumn(name = "assigned_to_user_id")
    private User assignedToUser; // For staff assignments

    @ManyToOne
    @JoinColumn(name = "assigned_to_room_id")
    private Room assignedToRoom; // For room assignments

    @Column(name = "assigned_to_location")
    private String assignedToLocation; // For simple location assignments (e.g., "Class 1")

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AssignmentPeriod assignmentPeriod = AssignmentPeriod.PERMANENT;

    @Column(nullable = false)
    private LocalDateTime startDate;

    private LocalDateTime endDate; // For temporary assignments

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AssignmentStatus status = AssignmentStatus.ACTIVE;


    @Column(name = "assigned_to_name", nullable = false)
    private String assignedToName;

    @ManyToOne
    @JoinColumn(name = "assigned_by_id", nullable = false)
    private User assignedBy;

    @Column(nullable = false)
    private LocalDateTime assignedAt;

    // Return tracking
    private LocalDateTime returnedAt;
    private String returnReason;

    @ManyToOne
    @JoinColumn(name = "returned_by_id")
    private User returnedBy;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // For equipment requests
    @ManyToOne
    @JoinColumn(name = "equipment_request_id")
    private EquipmentRequest equipmentRequest;

    public enum AssignmentType {
        STAFF_ASSIGNMENT,
        ROOM_ASSIGNMENT,
        LOCATION_ASSIGNMENT,
        REQUEST_ASSIGNMENT
    }

    public enum AssignmentPeriod {
        PERMANENT,
        TEMPORARY
    }

    public enum AssignmentStatus {
        ACTIVE,
        RETURNED,
        TRANSFERRED
    }

    public EquipmentAssignment(EquipmentUnit equipmentUnit, AssignmentType assignmentType, User assignedBy) {
        this.equipmentUnit = equipmentUnit;
        this.assignmentType = assignmentType;
        this.assignedBy = assignedBy;
        this.assignedAt = LocalDateTime.now();
        this.startDate = LocalDateTime.now();
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Helper methods
    public String getAssignedToName() {
        return this.assignedToName;
    }

    public boolean isActive() {
        return status == AssignmentStatus.ACTIVE;
    }

    public boolean isTemporary() {
        return assignmentPeriod == AssignmentPeriod.TEMPORARY;
    }

    public boolean isExpired() {
        return isTemporary() && endDate != null && endDate.isBefore(LocalDateTime.now());
    }


public void setAssignedToName(String assignedToName) {
    this.assignedToName = assignedToName;
}


}