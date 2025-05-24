package com.auca.library.dto.request;

import com.auca.library.model.RoomCategory;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Set;

@Data
public class RoomCreateRequest {
    @NotBlank(message = "Room number is required")
    private String roomNumber;

    @NotBlank(message = "Room name is required")
    private String name;

    private String description;

    @NotNull(message = "Room category is required")
    private RoomCategory category;

    @NotNull(message = "Capacity is required")
    @Min(value = 1, message = "Capacity must be at least 1")
    private Integer capacity;

    @NotNull(message = "Max booking hours is required")
    @Min(value = 1, message = "Max booking hours must be at least 1")
    private Integer maxBookingHours;

    @Min(value = 1, message = "Max bookings per day must be at least 1")
    private Integer maxBookingsPerDay = 1;

    @Min(value = 1, message = "Advance booking days must be at least 1")
    private Integer advanceBookingDays = 7;

    private boolean available = true;

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
    private boolean requiresApproval = false;
}
