package com.auca.library.dto.response;

import lombok.Data;

@Data
public class EquipmentResponse {
    private Long id;
    private String name;
    private String description;
    private boolean available;
}