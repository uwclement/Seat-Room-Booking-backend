package com.auca.library.dto.response;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class BookingDetailsResponse {
    private Long bookingId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String status;
    private String notes;
}