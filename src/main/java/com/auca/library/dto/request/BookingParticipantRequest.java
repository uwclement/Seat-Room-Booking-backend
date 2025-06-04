package com.auca.library.dto.request;

import java.util.List;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class BookingParticipantRequest {
     @NotNull(message = "Booking ID is required")
    private Long bookingId;

    private List<String> userEmails;
    private List<Long> userIds;
    private String invitationMessage;
}
