package com.auca.library.dto.response;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class BookingResponse {
    private Long id;
    private Long userId;
    private String userName;
    private Long seatId;
    private String identifier; 
    private String seatNumber;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String status;
    private LocalDateTime checkinTime;
    private LocalDateTime checkoutTime;
    private LocalDateTime cancellationTime;
    private String cancellationReason;
}