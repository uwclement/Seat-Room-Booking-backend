package com.auca.library.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;


@Data
public class BookingApprovalRequest {
    @NotNull(message = "Booking ID is required")
    private Long bookingId;

    @NotNull(message = "Approval status is required")
    private boolean approved;

    private String rejectionReason; // Required if approved = false
    private String notes;
}
