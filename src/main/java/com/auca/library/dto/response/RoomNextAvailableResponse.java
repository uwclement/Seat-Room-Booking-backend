package com.auca.library.dto.response;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class RoomNextAvailableResponse {
    private RoomResponse room;
    private boolean currentlyBooked;
    private LocalDateTime currentBookingEndTime;
    private LocalDateTime nextAvailableTime;
    private Integer durationHours;
    private Long hoursUntilAvailable;
}