package com.auca.library.dto.request;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SeatAvailabilityRequest {
    @NotNull
    @Future
    private LocalDateTime startTime;
    
    @NotNull
    @Future
    private LocalDateTime endTime;
    
    private String zoneType;
    
    private Boolean hasDesktop;
}