package com.auca.library.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class JoinBookingRequest {
     @NotNull(message = "Booking ID is required")
    private Long bookingId;

    private String message; // Optional message to organizer
    
}
