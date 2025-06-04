package com.auca.library.dto.response;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class BookingParticipantResponse {
    private Long id;
    private UserResponse user;
    private ParticipantStatus status;
    private LocalDateTime invitedAt;
    private LocalDateTime respondedAt;
    private LocalDateTime checkedInAt;
    private boolean notificationSent;
    
    public enum ParticipantStatus {
        INVITED, ACCEPTED, DECLINED, REMOVED
    }
}
