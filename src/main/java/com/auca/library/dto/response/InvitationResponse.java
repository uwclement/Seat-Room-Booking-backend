package com.auca.library.dto.response;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class InvitationResponse {
    private Long participantId;         // For accept/decline actions
    private RoomBookingResponse booking; // Full booking details
    private LocalDateTime invitedAt;
    private String inviterName;         // Who sent the invitation
}