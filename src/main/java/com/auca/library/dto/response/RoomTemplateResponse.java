package com.auca.library.dto.response;

import com.auca.library.model.RoomCategory;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Set;

@Data
public class RoomTemplateResponse {
    private Long id;
    private String templateName;
    private String description;
    private RoomCategory category;
    private Integer capacity;
    private Integer maxBookingHours;
    private Integer maxBookingsPerDay;
    private Integer advanceBookingDays;
    private boolean requiresApproval;
    private Set<EquipmentResponse> defaultEquipment;
    private LocalDateTime createdAt;
}
