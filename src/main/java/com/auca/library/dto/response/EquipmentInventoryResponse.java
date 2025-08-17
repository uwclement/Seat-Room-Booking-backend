package com.auca.library.dto.response;

import com.auca.library.model.EquipmentStatus;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EquipmentInventoryResponse {
    private Long id;
    private EquipmentStatus status;
    private Integer quantity;
    private String statusDisplayName;
    
    public String getStatusDisplayName() {
        return status != null ? status.getDisplayName() : "";
    }
}