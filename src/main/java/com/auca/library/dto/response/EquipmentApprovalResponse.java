package com.auca.library.dto.response;

import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
public class EquipmentApprovalResponse {
    private Long equipmentId;
    private String equipmentName;
    private Boolean approved;
    private String rejectionReason;
    private LocalDateTime decidedAt;
    private String decidedBy; // Admin name
}