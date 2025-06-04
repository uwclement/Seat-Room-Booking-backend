package com.auca.library.dto.response;

import lombok.Data;
import java.util.List;

@Data
public class BookingStatsResponse {
    private int periodDays;
    private int totalBookings;
    private int pendingBookings;
    private int confirmedBookings;
    private int cancelledBookings;
    private int completedBookings;
    private int rejectedBookings;
    private double averageBookingDuration;
    private double averageBookingsPerDay;
    private double approvalRate;
    private List<RoomBookingCountResponse> mostPopularRooms;
    private List<Integer> peakHours;
}