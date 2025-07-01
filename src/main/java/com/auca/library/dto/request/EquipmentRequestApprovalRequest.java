package com.auca.library.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EquipmentRequestApprovalRequest {
    private boolean approved;
    private String rejectionReason;
    private String adminSuggestion;
}
