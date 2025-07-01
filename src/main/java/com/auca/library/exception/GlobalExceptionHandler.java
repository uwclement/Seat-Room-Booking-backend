package com.auca.library.exception;

import org.apache.coyote.BadRequestException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<?> resourceNotFoundException(ResourceNotFoundException ex, WebRequest request) {
        ErrorDetails errorDetails = new ErrorDetails(
                new Date(),
                ex.getMessage(),
                request.getDescription(false));
        return new ResponseEntity<>(errorDetails, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ResponseEntity<?> emailAlreadyExistsException(EmailAlreadyExistsException ex, WebRequest request) {
        ErrorDetails errorDetails = new ErrorDetails(
                new Date(),
                ex.getMessage(),
                request.getDescription(false));
        return new ResponseEntity<>(errorDetails, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<?> badCredentialsException(BadCredentialsException ex, WebRequest request) {
        ErrorDetails errorDetails = new ErrorDetails(
                new Date(),
                "Invalid email or password",
                request.getDescription(false));
        return new ResponseEntity<>(errorDetails, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<?> badRequestException(BadRequestException ex, WebRequest request) {
        ErrorDetails errorDetails = new ErrorDetails(
                new Date(),
                ex.getMessage(),
                request.getDescription(false));
        return new ResponseEntity<>(errorDetails, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error -> {
            errors.put(error.getField(), error.getDefaultMessage());
        });
        
        ValidationErrorResponse errorResponse = new ValidationErrorResponse(
                new Date(),
                "Validation failed",
                errors);
        
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(BookingConflictException.class)
    public ResponseEntity<?> handleBookingConflictException(BookingConflictException ex, WebRequest request) {
        ErrorDetails errorDetails = new ErrorDetails(
                new Date(),
                ex.getMessage(),
                request.getDescription(false));
        return new ResponseEntity<>(errorDetails, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(BookingLimitExceededException.class)
    public ResponseEntity<?> handleBookingLimitExceededException(BookingLimitExceededException ex, WebRequest request) {
        // Create enhanced error response for limit exceptions
        Map<String, Object> details = new HashMap<>();
        details.put("timestamp", new Date());
        details.put("message", ex.getMessage());
        details.put("path", request.getDescription(false));
        
        // Add limit-specific details if available
        if (ex.getLimitType() != null) {
            details.put("limitType", ex.getLimitType());
            details.put("currentValue", ex.getCurrentValue());
            details.put("maxAllowed", ex.getMaxAllowed());
        }
        
        return new ResponseEntity<>(details, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(BookingValidationException.class)
    public ResponseEntity<?> handleBookingValidationException(BookingValidationException ex, WebRequest request) {
        // Create enhanced error response for validation exceptions
        Map<String, Object> details = new HashMap<>();
        details.put("timestamp", new Date());
        details.put("message", ex.getMessage());
        details.put("path", request.getDescription(false));
        
        // Add field validation details if available
        if (ex.getField() != null) {
            details.put("field", ex.getField());
            details.put("rejectedValue", ex.getRejectedValue());
        }
        
        return new ResponseEntity<>(details, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(RoomNotAvailableException.class)
    public ResponseEntity<?> handleRoomNotAvailableException(RoomNotAvailableException ex, WebRequest request) {
        // Create enhanced error response for room availability exceptions
        Map<String, Object> details = new HashMap<>();
        details.put("timestamp", new Date());
        details.put("message", ex.getMessage());
        details.put("path", request.getDescription(false));
        
        // Add room-specific details if available
        if (ex.getRoomId() != null) {
            details.put("roomId", ex.getRoomId());
            details.put("reason", ex.getReason());
        }
        
        return new ResponseEntity<>(details, HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(UnauthorizedBookingException.class)
    public ResponseEntity<?> handleUnauthorizedBookingException(UnauthorizedBookingException ex, WebRequest request) {
        // Create enhanced error response for authorization exceptions
        Map<String, Object> details = new HashMap<>();
        details.put("timestamp", new Date());
        details.put("message", ex.getMessage());
        details.put("path", request.getDescription(false));
        
        // Add authorization details
        if (ex.getUserEmail() != null) {
            details.put("userEmail", ex.getUserEmail());
            details.put("action", ex.getAction());
            details.put("bookingId", ex.getBookingId());
        }
        
        return new ResponseEntity<>(details, HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(CheckInException.class)
    public ResponseEntity<?> handleCheckInException(CheckInException ex, WebRequest request) {
        // Create enhanced error response for check-in exceptions
        Map<String, Object> details = new HashMap<>();
        details.put("timestamp", new Date());
        details.put("message", ex.getMessage());
        details.put("path", request.getDescription(false));
        
        // Add check-in specific details if available
        if (ex.getBookingId() != null) {
            details.put("bookingId", ex.getBookingId());
            details.put("checkInWindow", ex.getCheckInWindow());
        }
        
        return new ResponseEntity<>(details, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<?> handleSecurityException(SecurityException ex, WebRequest request) {
        ErrorDetails errorDetails = new ErrorDetails(
                new Date(),
                ex.getMessage(),
                request.getDescription(false));
        return new ResponseEntity<>(errorDetails, HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<?> handleIllegalStateException(IllegalStateException ex, WebRequest request) {
        ErrorDetails errorDetails = new ErrorDetails(
                new Date(),
                ex.getMessage(),
                request.getDescription(false));
        return new ResponseEntity<>(errorDetails, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<?> handleIllegalArgumentException(IllegalArgumentException ex, WebRequest request) {
        ErrorDetails errorDetails = new ErrorDetails(
                new Date(),
                ex.getMessage(),
                request.getDescription(false));
        return new ResponseEntity<>(errorDetails, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> globalExceptionHandler(Exception ex, WebRequest request) {
        ErrorDetails errorDetails = new ErrorDetails(
                new Date(),
                "An unexpected error occurred: " + ex.getMessage(),
                request.getDescription(false));
        return new ResponseEntity<>(errorDetails, HttpStatus.INTERNAL_SERVER_ERROR);
    }

}