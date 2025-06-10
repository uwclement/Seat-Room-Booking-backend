package com.auca.library.dto.response;

import java.time.LocalDateTime;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EnhancedAdminBookingResponse extends AdminBookingResponse {
    // Equipment approval info
    private List<EquipmentApprovalResponse> equipmentApprovals;
    private Boolean hasEquipmentRequests;
    private Integer pendingEquipmentCount;
    
    // Participant summary
    private ParticipantSummaryResponse participantSummary;
    
    // Capacity warnings
    private Boolean hasCapacityWarning;
    private String capacityWarningMessage;
    
    // Admin actions
    private Boolean canApproveEquipment;
    private Boolean canCancelBooking;
    private String adminCancellationReason; // If cancelled by admin
    private LocalDateTime adminCancelledAt;
    private String adminCancelledBy;
}