package com.auca.library.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.FORBIDDEN)
public class RoomNotAvailableException extends RuntimeException {
    
    private Long roomId;
    private String reason;
    
    public RoomNotAvailableException(String message) {
        super(message);
    }
    
    public RoomNotAvailableException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public RoomNotAvailableException(Long roomId, String reason) {
        super(String.format("Room %d is not available: %s", roomId, reason));
        this.roomId = roomId;
        this.reason = reason;
    }
    
    // Getters
    public Long getRoomId() {
        return roomId;
    }
    
    public String getReason() {
        return reason;
    }
}