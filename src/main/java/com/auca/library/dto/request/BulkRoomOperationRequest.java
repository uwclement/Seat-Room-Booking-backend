package com.auca.library.dto.request;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Data
public class BulkRoomOperationRequest {
    private List<Long> roomIds;
    private String operation; // "enable", "disable", "set_maintenance", "clear_maintenance", "add_equipment", "remove_equipment"
    
    // For maintenance operations
    private LocalDateTime maintenanceStart;
    private LocalDateTime maintenanceEnd;
    private String maintenanceNotes;
    
    // For equipment operations
    private Set<Long> equipmentIds;
    
    // For availability operations
    private Boolean available;
}