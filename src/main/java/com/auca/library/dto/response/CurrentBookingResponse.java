package com.auca.library.dto.response;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class CurrentBookingResponse {
    private Long roomId;
    private LocalDateTime currentTime;
    private boolean hasCurrentBooking;
    private RoomBookingResponse booking;
    private Long timeRemaining; // minutes
    private boolean canCheckIn;
    private boolean checkedIn;
    private LocalDateTime nextBookingTime;
    private Long timeUntilNext; // minutes
}