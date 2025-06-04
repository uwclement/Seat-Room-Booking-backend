package com.auca.library.dto.response;

import lombok.Data;
import java.time.LocalDate;

@Data
public class DailyBookingAnalyticsResponse {
    private LocalDate date;
    private int totalBookings;
    private int confirmedBookings;
    private int pendingBookings;
    private int cancelledBookings;
    private double averageDuration;
    private int uniqueUsers;
    private int uniqueRooms;
}