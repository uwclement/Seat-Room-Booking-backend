package com.auca.library.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ExtensionRequest {
    @NotNull
    private Long bookingId;
    
    @NotNull
    private boolean extend; // true to extend, false to decline
}