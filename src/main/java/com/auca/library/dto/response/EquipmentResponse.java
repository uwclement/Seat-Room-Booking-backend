package com.auca.library.dto.response;

import java.util.List;

import com.auca.library.model.Location;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EquipmentResponse {
    private Long id;
    private String name;
    private String description;
    private boolean available;
    private boolean allowedToStudents;
    private Integer quantity;
    private Integer availableQuantity;
    private Location location;
    private String locationDisplayName;
    
    // NEW: Inventory breakdown by status
    private List<EquipmentInventoryResponse> inventoryBreakdown;
    private Integer totalQuantity;
    private Integer actualAvailableQuantity;
    
    public String getLocationDisplayName() {
        return location != null ? location.getDisplayName() : "";
    }
}