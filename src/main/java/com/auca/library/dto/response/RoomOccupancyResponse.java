package com.auca.library.dto.response;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class RoomOccupancyResponse {
    private RoomResponse room;
    private boolean occupied;
    private RoomBookingResponse currentBooking;
    private LocalDateTime occupiedUntil;
    private Long occupiedFor; // minutes
    private LocalDateTime nextBookingTime;
    private Long freeFor; // minutes until next booking
}