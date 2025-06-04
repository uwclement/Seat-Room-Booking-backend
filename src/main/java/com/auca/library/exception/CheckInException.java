package com.auca.library.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.BAD_REQUEST)
public class CheckInException extends RuntimeException {
    
    private Long bookingId;
    private String checkInWindow;
    
    public CheckInException(String message) {
        super(message);
    }
    
    public CheckInException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public CheckInException(Long bookingId, String checkInWindow, String message) {
        super(String.format("Check-in failed for booking %d (window: %s): %s", 
              bookingId, checkInWindow, message));
        this.bookingId = bookingId;
        this.checkInWindow = checkInWindow;
    }
    
    // Getters
    public Long getBookingId() {
        return bookingId;
    }
    
    public String getCheckInWindow() {
        return checkInWindow;
    }
}