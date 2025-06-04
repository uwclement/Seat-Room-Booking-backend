package com.auca.library.dto.response;

import java.time.LocalDateTime;

import lombok.Data;


@Data
public class WaitlistResponse {
        private Long id;
    private UserResponse user;
    private RoomResponse room;
    private LocalDateTime desiredStartTime;
    private LocalDateTime desiredEndTime;
    private Integer priority;
    private Integer positionInQueue;
    private boolean isActive;
    private LocalDateTime estimatedAvailabilityTime;
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;
}
