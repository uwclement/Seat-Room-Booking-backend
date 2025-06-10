package com.auca.library.dto.request;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BulkAdminCancellationRequest {
    @NotEmpty
    private List<Long> bookingIds;
    
    @NotBlank
    private String cancellationReason;
    
    private Boolean notifyParticipants = true;
}