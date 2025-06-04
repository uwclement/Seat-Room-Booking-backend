package com.auca.library.dto.request;

import lombok.Data;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;

@Data
public class ExtendBookingRequest {
    @NotNull(message = "Additional hours is required")
    @Min(value = 1, message = "Must extend by at least 1 hour")
    @Max(value = 4, message = "Cannot extend by more than 4 hours")
    private Integer additionalHours;
    
    private String reason;
}