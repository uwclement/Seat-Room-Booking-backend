package com.auca.library.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;
import java.util.List;

@Getter
@Setter
public class LabClassRequest {
    @NotBlank(message = "Lab number is required")
    private String labNumber;
    
    @NotBlank(message = "Name is required")
    private String name;
    
    private String description;
    
    @Positive(message = "Capacity must be positive")
    private Integer capacity;
    
    @NotBlank(message = "Building is required")
    private String building;
    
    @NotBlank(message = "Floor is required")
    private String floor;
    
    private List<Long> equipmentIds;
}