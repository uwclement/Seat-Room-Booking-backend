package com.auca.library.dto.request;

import java.util.Set;

import com.auca.library.model.RoomCategory;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RoomTemplateRequest {
    @NotBlank(message = "Template name is required")
    private String templateName;

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

    private boolean requiresApproval = false;

    // Default equipment IDs for this template
    private Set<Long> defaultEquipmentIds;
}