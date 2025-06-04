package com.auca.library.dto.response;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class BookingAlertResponse {
    private String type; // OVERDUE_APPROVAL, NO_SHOW, HIGH_PENDING_VOLUME, etc.
    private String message;
    private Long bookingId;
    private String severity; // LOW, MEDIUM, HIGH, CRITICAL
    private LocalDateTime createdAt;
    private String actionRequired;
    private String actionUrl;
}