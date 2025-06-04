package com.auca.library.dto.request;

import java.time.LocalDateTime;
import java.util.Set;

import lombok.Data;

@Data
public class BookingUpdateRequest {
    private String title;
    private String description;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer maxParticipants;
    private Boolean isPublic;
    private Boolean allowJoining;
    private Boolean reminderEnabled;
    private Set<Long> requestedEquipmentIds;
}
