package com.auca.library.dto.request;

import com.auca.library.model.Location;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EquipmentRequest {
    public static final String RequestStatus = null;

    @NotBlank
    private String name;
    
    private String description;
    
    private boolean available = true;
    
    private boolean allowedToStudents = false;
    
    @Min(1)
    private Integer quantity;
    
    @NotNull
    private Location location;
}