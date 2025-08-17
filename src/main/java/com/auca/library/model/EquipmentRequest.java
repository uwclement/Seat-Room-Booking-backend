package com.auca.library.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "equipment_requests")
@Getter
@Setter
@NoArgsConstructor
public class EquipmentRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "equipment_id", nullable = false)
    private Equipment equipment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id")
    private Course course; // Required for professors

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lab_class_id")
    private LabClass labClass; // If requesting lab class

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_booking_id")
    private RoomBooking roomBooking; // If part of room booking

    @Column(nullable = false)
    private String reason;

    @Column(nullable = false)
    private LocalDateTime startTime;

    @Column(nullable = false)
    private LocalDateTime endTime;

    @Column(nullable = false)
    private Integer requestedQuantity = 1;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RequestStatus status = RequestStatus.PENDING;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by")
    private User approvedBy;

    private LocalDateTime approvedAt;
    private String rejectionReason;
    private String adminSuggestion;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime updatedAt = LocalDateTime.now();

    // Escalation fields
    private boolean escalatedToHod = false;
    private LocalDateTime escalatedAt;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hod_reviewed_by")
    private User hodReviewedBy;
    private LocalDateTime hodReviewedAt;

    @Column(name = "suggestion_acknowledged")
    private Boolean suggestionAcknowledged; // null = no response, true = acknowledged, false = rejected

    @Column(name = "suggestion_response_reason", length = 500)
    private String suggestionResponseReason;

    @Column(name = "suggestion_response_at")
    private LocalDateTime suggestionResponseAt;

    // Return fields
    @Column(name = "returned_at")
    private LocalDateTime returnedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "returned_by_id")
    private User returnedBy;

    @Column(name = "return_condition", length = 50)
    private String returnCondition; // GOOD, DAMAGED, MISSING_PARTS, LOST

    @Column(name = "return_notes", length = 1000)
    private String returnNotes;

    @Column(name = "is_early_return")
    private Boolean isEarlyReturn = false;

    @Column(name = "is_late_return")
    private Boolean isLateReturn = false;

    // Extension fields
    @Column(name = "total_extensions_today")
    private Integer totalExtensionsToday = 0;

    @Column(name = "total_extension_hours_today")
    private Double totalExtensionHoursToday = 0.0;

    @Column(name = "extension_reason", length = 500)
    private String extensionReason;

    @Column(name = "extension_status", length = 20)
    private String extensionStatus; // PENDING, APPROVED, REJECTED

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "extension_approved_by_id")
    private User extensionApprovedBy;

    @Column(name = "extension_requested_at")
    private LocalDateTime extensionRequestedAt;

    @Column(name = "extension_approved_at")
    private LocalDateTime extensionApprovedAt;

    @Column(name = "extension_hours_requested")
    private Double extensionHoursRequested;

    @Column(name = "original_end_time")
    private LocalDateTime originalEndTime;

    // NEW FIELD: Equipment Unit Assignment - ADDED FOR SERIAL NUMBER TRACKING
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_equipment_unit_id")
    private EquipmentUnit assignedEquipmentUnit;

    public enum RequestStatus {
        PENDING,
        APPROVED,
        REJECTED,
        ESCALATED,
        HOD_APPROVED,
        HOD_REJECTED,
        IN_USE,
        EXTENSION_REQUESTED,
        EXTENSION_APPROVED,
        EXTENSION_REJECTED,
        RETURNED,
        COMPLETED,
        CANCELLED
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Helper methods for equipment unit management
    public boolean hasAssignedEquipmentUnit() {
        return assignedEquipmentUnit != null;
    }

    public String getAssignedSerialNumber() {
        return assignedEquipmentUnit != null ? assignedEquipmentUnit.getSerialNumber() : null;
    }

    public boolean isEquipmentAssigned() {
        return hasAssignedEquipmentUnit();
    }

    // Helper methods for status checking
    public boolean isPending() {
        return status == RequestStatus.PENDING;
    }

    public boolean isApproved() {
        return status == RequestStatus.APPROVED || status == RequestStatus.HOD_APPROVED;
    }

    public boolean isRejected() {
        return status == RequestStatus.REJECTED || status == RequestStatus.HOD_REJECTED;
    }

    public boolean isCompleted() {
        return status == RequestStatus.COMPLETED;
    }

    public boolean isCancelled() {
        return status == RequestStatus.CANCELLED;
    }

    public boolean isInUse() {
        return status == RequestStatus.IN_USE;
    }

    public boolean isReturned() {
        return status == RequestStatus.RETURNED;
    }

    // Helper methods for extension management
    public boolean hasActiveExtension() {
        return extensionStatus != null && !extensionStatus.equals("REJECTED");
    }

    public boolean canRequestExtension() {
        return (extensionStatus == null || extensionStatus.equals("REJECTED")) &&
               (status == RequestStatus.APPROVED || status == RequestStatus.IN_USE) &&
               getRemainingExtensionHours() > 0;
    }

    public double getRemainingExtensionHours() {
        return 3.0 - (totalExtensionHoursToday != null ? totalExtensionHoursToday : 0.0);
    }

    // Helper methods for return management
    public boolean canBeReturned() {
        return returnedAt == null && 
               (status == RequestStatus.APPROVED ||
                status == RequestStatus.IN_USE ||
                status == RequestStatus.HOD_APPROVED);
    }

    public boolean isOverdue() {
        return endTime != null && endTime.isBefore(LocalDateTime.now()) && 
               returnedAt == null && 
               (status == RequestStatus.APPROVED || status == RequestStatus.IN_USE);
    }

    // Helper methods for escalation
    public boolean canEscalateToHod() {
        return status == RequestStatus.REJECTED && !escalatedToHod;
    }

    public boolean isEscalated() {
        return status == RequestStatus.ESCALATED;
    }

    // Helper methods for suggestion response
    public boolean canRespondToSuggestion() {
        return adminSuggestion != null && 
               suggestionAcknowledged == null && 
               status != RequestStatus.HOD_REJECTED;
    }

    public boolean hasSuggestionResponse() {
        return suggestionAcknowledged != null;
    }

    // Helper method for equipment location validation
    public boolean isEquipmentInSameLocation(User user) {
        return equipment != null && equipment.getLocation().equals(user.getLocation());
    }

    // Helper method for duration calculation
    public long getDurationHours() {
        if (startTime != null && endTime != null) {
            return java.time.Duration.between(startTime, endTime).toHours();
        }
        return 0L;
    }

    // Helper method for equipment validation
    public boolean isValidForUser(User user) {
        // Students can only request equipment allowed for students
        if (user.getRoles().stream().anyMatch(role -> role.getName().name().equals("ROLE_USER"))) {
            return equipment != null && equipment.isAllowedToStudents();
        }
        return true; // Professors and staff can request any equipment
    }

    // Helper method for time validation
    public boolean isValidTimeSlot() {
        return startTime != null && endTime != null && startTime.isBefore(endTime);
    }

    // Helper method for future booking validation
    public boolean isScheduledForFuture() {
        return startTime != null && startTime.isAfter(LocalDateTime.now());
    }

    // Helper method for current time validation
    public boolean isCurrentlyActive() {
        LocalDateTime now = LocalDateTime.now();
        return startTime != null && endTime != null && 
               !startTime.isAfter(now) && !endTime.isBefore(now);
    }

    // Helper method for equipment availability check
    public boolean isQuantityAvailable() {
        return equipment != null && equipment.isAvailableInQuantity(requestedQuantity);
    }

    // toString method for debugging
    @Override
    public String toString() {
        return String.format("EquipmentRequest{id=%d, equipment='%s', user='%s', status=%s, serialNumber='%s'}",
                id, 
                equipment != null ? equipment.getName() : "null",
                user != null ? user.getFullName() : "null",
                status,
                getAssignedSerialNumber());
    }
}