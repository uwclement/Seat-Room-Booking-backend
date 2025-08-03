package com.auca.library.dto.request;

import java.time.LocalDateTime;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LabRequestRequest {
    @NotNull
    private Long labClassId;
    
    @NotNull
    private Long courseId;
    
    @NotBlank
    private String reason;
    
    @NotNull
    private LocalDateTime startTime;
    
    @NotNull
    private LocalDateTime endTime;
}