package com.auca.library.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.BAD_REQUEST)
public class BookingValidationException extends RuntimeException {
    
    private String field;
    private Object rejectedValue;
    
    public BookingValidationException(String message) {
        super(message);
    }
    
    public BookingValidationException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public BookingValidationException(String field, Object rejectedValue, String message) {
        super(String.format("Validation failed for field '%s' with value '%s': %s", 
              field, rejectedValue, message));
        this.field = field;
        this.rejectedValue = rejectedValue;
    }
    
    // Getters
    public String getField() {
        return field;
    }
    
    public Object getRejectedValue() {
        return rejectedValue;
    }
}