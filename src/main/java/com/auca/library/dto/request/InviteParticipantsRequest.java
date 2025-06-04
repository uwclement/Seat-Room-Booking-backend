package com.auca.library.dto.request;

import lombok.Data;
import java.util.List;

@Data
public class InviteParticipantsRequest {
    private List<String> invitedEmails;
    private List<Long> invitedUserIds;
    private String message;
    
    // At least one invitation method should be provided
    public boolean hasInvitations() {
        return (invitedEmails != null && !invitedEmails.isEmpty()) ||
               (invitedUserIds != null && !invitedUserIds.isEmpty());
    }
}
