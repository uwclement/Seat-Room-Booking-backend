package com.auca.library.dto.response;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class BookedRoomResponse {
    private RoomResponse room;
    private RoomBookingResponse currentBooking;
    private LocalDateTime nextAvailableTime;
    private Integer upcomingBookingsCount;
    private Long hoursUntilAvailable;
}