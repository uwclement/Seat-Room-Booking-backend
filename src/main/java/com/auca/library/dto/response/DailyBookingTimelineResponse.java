package com.auca.library.dto.response;

import lombok.Data;
import java.time.LocalDate;
import java.util.List;

@Data
public class DailyBookingTimelineResponse {
    private LocalDate date;
    private List<TimelineBookingResponse> bookings;
    private List<AvailableSlotResponse> availableSlots;
    private Double totalBookedHours;
    private Double utilizationPercentage;
}
