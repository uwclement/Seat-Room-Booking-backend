package com.auca.library.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.CONFLICT)
public class BookingConflictException extends RuntimeException {
    
    public BookingConflictException(String message) {
        super(message);
    }
    
    public BookingConflictException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public BookingConflictException(String message, Long roomId, String timeSlot) {
        super(String.format("%s - Room ID: %d, Time: %s", message, roomId, timeSlot));
    }
}