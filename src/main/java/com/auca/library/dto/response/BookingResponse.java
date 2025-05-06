package com.auca.library.dto.response;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class BookingResponse {
    private Long id;
    private Long userId;
    private String userName;
    private Long seatId;
    private String seatNumber;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String status;
    private LocalDateTime checkinTime;
    private LocalDateTime checkoutTime;
    private LocalDateTime cancellationTime;
    private String cancellationReason;
}