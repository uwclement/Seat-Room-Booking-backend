package com.auca.library.dto.response;

import java.time.LocalDateTime;

import com.auca.library.model.Booking;

import lombok.Data;

@Data
public class BookingDTO {
    private Long id;
    private Long userId;
    private String userName;
    private Long seatId;
    private String studentId;
    private String seatNumber;
    private String zoneType;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private LocalDateTime createdAt;
    private Booking.BookingStatus status;
    private boolean checkedIn;
    private LocalDateTime checkedInTime;
    private LocalDateTime checkedOutTime;
    private boolean extended;
    private boolean extensionRequested;
    private LocalDateTime extensionNotifiedAt;
    private String notes;
    private long remainingMinutes; // For calculating extension time
}
