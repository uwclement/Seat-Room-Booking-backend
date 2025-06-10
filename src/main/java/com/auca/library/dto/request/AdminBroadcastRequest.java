package com.auca.library.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import java.util.List;

@Getter
@Setter
public class AdminBroadcastRequest {
    @NotBlank
    private String title;
    
    @NotBlank
    private String message;
    
    private BroadcastTarget target = BroadcastTarget.ACTIVE_BOOKING_USERS;
    private List<String> specificUserEmails;
    
    public enum BroadcastTarget {
        ALL_USERS,
        ACTIVE_BOOKING_USERS,
        RECENT_BOOKING_USERS,
        SPECIFIC_USERS
    }
}