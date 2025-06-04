package com.auca.library.dto.response;

import lombok.Data;
import java.util.List;

@Data
public class RoomCalendarResponse {
    private RoomResponse room;
    private List<CalendarBookingResponse> bookings;
    private Double utilizationPercentage;
}