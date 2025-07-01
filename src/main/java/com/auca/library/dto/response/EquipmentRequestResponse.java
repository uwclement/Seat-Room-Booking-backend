package com.auca.library.dto.response;

import com.auca.library.model.EquipmentRequest;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

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
}