package com.auca.library.dto.response;

import lombok.Data;

@Data
public class EquipmentResponse {
    private Long id;
    private String name;
    private String description;
    private boolean available;

    private boolean allowedToStudents;
    private Integer quantity;
    private Integer availableQuantity;
    private Integer reservedQuantity; // Calculated field
    
    // Usage statistics
    private int totalRooms; 
    private int totalLabClasses; 
    private boolean hasActiveRequests; 
}