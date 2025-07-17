package com.auca.library.dto.response;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import com.auca.library.model.RoomBooking;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class RoomBookingResponse {
    private Long id;
    private RoomResponse room;
    private UserResponse user;
    private String title;
    private String description;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private RoomBooking.BookingStatus status;
    private Integer maxParticipants;

    @JsonProperty("isPublic")
    private boolean isPublic;
    
    private boolean allowJoining;
    private boolean requiresCheckIn;
    private LocalDateTime checkedInAt;
    private boolean reminderEnabled;
    private LocalDateTime reminderSentAt;
    
    // Participants
    private List<BookingParticipantResponse> participants;
    private Integer checkedInCount;
    private Integer totalParticipants;
    private List<String> invitedUserIdentifiers; 
    
    // Equipment
    private Set<EquipmentResponse> requestedEquipment;
    
    // Recurring info
    private RecurringBookingSeriesResponse recurringInfo;
    
    // Admin fields
    private boolean requiresApproval;
    private UserResponse approvedBy;
    private LocalDateTime approvedAt;
    private String rejectionReason;
    
    // Timestamps
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Helper fields
    private boolean canCheckIn;
    private boolean isOverdue;
    private boolean canJoin;
    private boolean canEdit;
    private boolean canCancel;
}