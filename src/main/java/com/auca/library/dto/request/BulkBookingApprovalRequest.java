package com.auca.library.dto.request;

import java.util.List;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class BulkBookingApprovalRequest {
    @NotNull(message = "Booking IDs are required")
    private List<Long> bookingIds;

    @NotNull(message = "Approval status is required")
    private boolean approved;

    private String rejectionReason;
    private String notes;
}
