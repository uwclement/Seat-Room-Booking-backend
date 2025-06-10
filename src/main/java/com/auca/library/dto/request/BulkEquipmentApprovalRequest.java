package com.auca.library.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import java.util.List;

@Getter
@Setter
public class BulkEquipmentApprovalRequest {
    @NotEmpty
    private List<Long> bookingIds;
    
    @NotEmpty
    private List<Long> equipmentIds;
    
    @NotNull
    private Boolean approved;
    
    private String reason; // Optional reason for rejection
}