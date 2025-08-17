package com.auca.library.dto.response;

import java.time.LocalDateTime;

import com.auca.library.model.EquipmentRequest;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EquipmentRequestResponse {
    private Long id;
    
    // Equipment details
    private Long equipmentId;
    private String equipmentName;
    private String equipmentDescription;
    
    // Request details
    private String reason;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer requestedQuantity;
    private EquipmentRequest.RequestStatus status;
    private String statusDisplayName; // Human-readable status

    private String assignedSerialNumber;
    private Long assignedUnitId;
    private String assignedUnitCondition;
    private boolean hasSerialNumberAssigned = false;
    
    // Approval/Rejection details
    private String rejectionReason;
    private String adminSuggestion;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime approvedAt;
    
    // Escalation details
    private boolean escalatedToHod;
    private LocalDateTime escalatedAt;
    private boolean canEscalate; // Whether user can escalate this request
    
    // User details
    private Long userId;
    private String userFullName;
    private String userEmail;
    
    // Course info (for professors)
    private Long courseId;
    private String courseCode;
    private String courseName;
    
    // Lab class info (if applicable)
    private Long labClassId;
    private String labClassName;
    
    // Room booking info (if applicable)
    private Long roomBookingId;
    private String roomBookingTitle;
    
    // Approval info
    private String approvedByName;
    private String approvedByEmail;
    private String hodReviewedByName;
    private LocalDateTime hodReviewedAt;
    
    // Calculated fields
    private long durationHours;
    private boolean isActive; // Whether request is currently in effect
    private boolean isExpired; // Whether request time has passed
    private int daysUntilStart; // Days until start time

     // Suggestion Response fields
    private Boolean suggestionAcknowledged; // null = no response, true = acknowledged, false = rejected
    private String suggestionResponseReason;
    private LocalDateTime suggestionResponseAt;
    private boolean canRespondToSuggestion; // Whether user can respond to admin suggestion
    
    // Return fields
    private LocalDateTime returnedAt;
    private String returnedByName;
    private String returnCondition; // GOOD, DAMAGED, MISSING_PARTS, LOST
    private String returnNotes;
    private boolean isEarlyReturn;
    private boolean isLateReturn;
    private boolean canMarkReturned; // Whether equipment admin can mark as returned
    
    // Extension fields
    private boolean hasActiveExtension;
    private Double totalExtensionsToday;
    private Double totalExtensionHoursToday;
    private Double remainingExtensionHours; // How many hours left for today (3.0 - used)
    private String extensionReason;
    private String extensionStatus; // PENDING, APPROVED, REJECTED
    private LocalDateTime extensionRequestedAt;
    private LocalDateTime extensionApprovedAt;
    private Double extensionHoursRequested;
    private String extensionApprovedByName;
    private LocalDateTime originalEndTime;
    private boolean canRequestExtension; // Whether user can request extension
    private boolean canApproveExtension; // Whether equipment admin can approve extension
    
    // Conflict information (for equipment admin when approving extensions)
    private boolean hasConflicts; // Whether extension would cause conflicts
    private int conflictingRequestsCount;
    private String conflictDetails; // Summary of conflicts
    
    // Action availability flags
    private boolean canCancel;
    private boolean canComplete;
    private boolean canEscalateToHod;
    private boolean showReturnSection; // Whether to show return form
    private boolean showExtensionSection; // Whether to show extension request form
}