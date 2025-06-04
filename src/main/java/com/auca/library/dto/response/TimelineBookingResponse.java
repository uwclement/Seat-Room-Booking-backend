package com.auca.library.dto.response;

import com.auca.library.model.RoomBooking;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class TimelineBookingResponse {
    private Long id;
    private String title;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private RoomBooking.BookingStatus status;
    private String organizerName;
    private Integer participantCount;
    private boolean publicBooking;
}