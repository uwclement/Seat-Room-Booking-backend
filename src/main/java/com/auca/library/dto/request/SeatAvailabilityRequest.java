package com.auca.library.dto.request;

import java.time.LocalDateTime;

import jakarta.validation.constraints.Future;
import lombok.Data;

@Data
public class SeatAvailabilityRequest {
    // @NotNull
    @Future
    private LocalDateTime startTime;
    
    // @NotNull
    @Future
    private LocalDateTime endTime;
    
    private String zoneType;
    
    private Boolean hasDesktop;

    // private Boolean isAvailabe;
}