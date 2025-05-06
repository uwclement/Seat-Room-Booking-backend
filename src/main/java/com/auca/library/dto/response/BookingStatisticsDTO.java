package com.auca.library.dto.response;

import lombok.Data;
import java.time.DayOfWeek;
import java.time.LocalTime;

@Data
public class BookingStatisticsDTO {
    private DayOfWeek dayOfWeek;
    private LocalTime hourOfDay;
    private int bookingCount;
    private double occupancyRate;
    private boolean isPeakTime;
}