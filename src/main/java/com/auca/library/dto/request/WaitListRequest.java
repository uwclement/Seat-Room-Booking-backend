package com.auca.library.dto.request;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class WaitListRequest {
    @NotNull
    private Long seatId;
    
    @NotNull
    @Future
    private LocalDateTime requestedStartTime;
    
    @NotNull
    @Future
    private LocalDateTime requestedEndTime;
}