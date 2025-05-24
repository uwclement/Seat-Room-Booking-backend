package com.auca.library.dto.response;

import java.time.LocalDateTime;
import java.util.Set;

import com.auca.library.model.RoomCategory;

import lombok.Data;

@Data
public class RoomResponse {
    private Long id;
    private String roomNumber;
    private String name;
    private String description;
    private RoomCategory category;
    private Integer capacity;
    private Integer maxBookingHours;
    private Integer maxBookingsPerDay;
    private Integer advanceBookingDays;
    private boolean available;
    private boolean requiresBooking;

    // Location details
    private String building;
    private String floor;
    private String department;

    // Equipment
    private Set<EquipmentResponse> equipment;

    // Maintenance info
    private LocalDateTime maintenanceStart;
    private LocalDateTime maintenanceEnd;
    private String maintenanceNotes;
    private boolean underMaintenance;

    // Approval settings
    private boolean requiresApproval;

    // Timestamps
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}