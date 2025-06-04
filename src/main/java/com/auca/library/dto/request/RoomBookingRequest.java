package com.auca.library.dto.request;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RoomBookingRequest {
    @NotNull(message = "Room ID is required")
    private Long roomId;

    @NotBlank(message = "Title is required")
    private String title;

    private String description;

    @NotNull(message = "Start time is required")
    @Future(message = "Start time must be in the future")
    private LocalDateTime startTime;

    @NotNull(message = "End time is required")
    private LocalDateTime endTime;

    @Min(value = 1, message = "Max participants must be at least 1")
    private Integer maxParticipants = 1;

    private boolean isPublic = false;
    private boolean allowJoining = false;
    private boolean requiresCheckIn = true;
    private boolean reminderEnabled = true;

    // Equipment requests
    private Set<Long> requestedEquipmentIds;

    // Participant invitations
    private List<String> invitedUserEmails;
    private List<Long> invitedUserIds;

    // For recurring bookings
    private boolean isRecurring = false;
    private RecurringBookingRequest recurringDetails;
}