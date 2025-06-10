package com.auca.library.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminBookingCancellationRequest {
    @NotNull
    private Long bookingId;
    
    @NotBlank
    private String cancellationReason;
    
    private Boolean notifyParticipants = true;
}