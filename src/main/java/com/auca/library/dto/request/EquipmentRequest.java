package com.auca.library.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class EquipmentRequest {
    @NotBlank(message = "Equipment name is required")
    private String name;

    private String description;

    private boolean available = true;

        private boolean allowedToStudents = false;
    
    @Positive(message = "Quantity must be positive")
    private Integer quantity = 1;
}