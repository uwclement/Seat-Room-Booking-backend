package com.auca.library.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.FORBIDDEN)
public class UnauthorizedBookingException extends RuntimeException {
    
    private String userEmail;
    private String action;
    private Long bookingId;
    
    public UnauthorizedBookingException(String message) {
        super(message);
    }
    
    public UnauthorizedBookingException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public UnauthorizedBookingException(String userEmail, String action, Long bookingId) {
        super(String.format("User '%s' is not authorized to '%s' booking ID: %d", 
              userEmail, action, bookingId));
        this.userEmail = userEmail;
        this.action = action;
        this.bookingId = bookingId;
    }
    
    // Getters
    public String getUserEmail() {
        return userEmail;
    }
    
    public String getAction() {
        return action;
    }
    
    public Long getBookingId() {
        return bookingId;
    }
}