package com.auca.library.dto.request;

import com.auca.library.model.RoomCategory;
import jakarta.validation.constraints.Min;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Set;

@Data
public class RoomUpdateRequest {
    private String name;
    private String description;
    private RoomCategory category;

    @Min(value = 1, message = "Capacity must be at least 1")
    private Integer capacity;

    @Min(value = 1, message = "Max booking hours must be at least 1")
    private Integer maxBookingHours;

    @Min(value = 1, message = "Max bookings per day must be at least 1")
    private Integer maxBookingsPerDay;

    @Min(value = 1, message = "Advance booking days must be at least 1")
    private Integer advanceBookingDays;

    private Boolean available;

    // Location details
    private String building;
    private String floor;
    private String department;

    // Equipment IDs to associate with this room
    private Set<Long> equipmentIds;

    // Maintenance window
    private LocalDateTime maintenanceStart;
    private LocalDateTime maintenanceEnd;
    private String maintenanceNotes;

    // Approval settings
    private Boolean requiresApproval;
}


