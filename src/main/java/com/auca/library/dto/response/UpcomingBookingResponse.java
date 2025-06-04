package com.auca.library.dto.response;

import com.auca.library.model.RoomBooking;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class UpcomingBookingResponse {
    private Long id;
    private String title;
    private String description;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private RoomBooking.BookingStatus status;
    private String organizerName;
    private Integer participantCount;
    private Long hoursFromNow;
    private boolean requiresCheckIn;
    private boolean publicBooking;
}