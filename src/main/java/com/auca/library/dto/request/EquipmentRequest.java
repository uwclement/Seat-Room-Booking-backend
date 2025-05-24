package com.auca.library.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class EquipmentRequest {
    @NotBlank(message = "Equipment name is required")
    private String name;

    private String description;

    private boolean available = true;
}