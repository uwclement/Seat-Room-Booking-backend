package com.auca.library.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.BAD_REQUEST)
public class BookingLimitExceededException extends RuntimeException {
    
    private String limitType;
    private Object currentValue;
    private Object maxAllowed;
    
    public BookingLimitExceededException(String message) {
        super(message);
    }
    
    public BookingLimitExceededException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public BookingLimitExceededException(String limitType, Object currentValue, Object maxAllowed) {
        super(String.format("%s limit exceeded. Current: %s, Max allowed: %s", 
              limitType, currentValue, maxAllowed));
        this.limitType = limitType;
        this.currentValue = currentValue;
        this.maxAllowed = maxAllowed;
    }
    
    public BookingLimitExceededException(String message, String limitType, Object currentValue, Object maxAllowed) {
        super(message);
        this.limitType = limitType;
        this.currentValue = currentValue;
        this.maxAllowed = maxAllowed;
    }
    
    // Getters
    public String getLimitType() {
        return limitType;
    }
    
    public Object getCurrentValue() {
        return currentValue;
    }
    
    public Object getMaxAllowed() {
        return maxAllowed;
    }
}
