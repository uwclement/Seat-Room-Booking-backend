package com.auca.library.dto.request;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotificationMessage {
    private String id;
    private String title;
    private String message;
    private String type;
    private Long timestamp;
    private Long expirationTime;
    private boolean read = false;
    private Map<String, Object> metadata = new HashMap<>();
    
    public NotificationMessage(String title, String message, String type, Long expirationHours) {
        this.id = UUID.randomUUID().toString();
        this.title = title;
        this.message = message;
        this.type = type;
        this.timestamp = System.currentTimeMillis();
        
        // Calculate expiration time based on hours from now
        this.expirationTime = LocalDateTime.now().plusHours(expirationHours)
                .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }
    
    // Helper method to check if notification is expired
    public boolean isExpired() {
        return System.currentTimeMillis() > expirationTime;
    }
}