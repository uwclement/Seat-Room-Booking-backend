package com.auca.library.dto.request;

import java.util.List;

import lombok.Data;

@Data
public class InviteParticipantsRequest {
    private List<String> invitedEmails;
    private List<Long> invitedUserIds;
    private String message;
    private List<String> invitedUserIdentifiers;
    
    // At least one invitation method should be provided
    public boolean hasInvitations() {
        return (invitedEmails != null && !invitedEmails.isEmpty()) ||
               (invitedUserIds != null && !invitedUserIds.isEmpty());
    }

    public List<String> getInvitedUserIdentifiers() {
    return invitedUserIdentifiers;
}

   public void setInvitedUserIdentifiers(List<String> invitedUserIdentifiers) {
    this.invitedUserIdentifiers = invitedUserIdentifiers;
}
}
