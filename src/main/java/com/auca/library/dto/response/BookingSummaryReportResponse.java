package com.auca.library.dto.response;

import com.auca.library.model.RoomBooking;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
public class BookingSummaryReportResponse {
    private int reportPeriod;
    private LocalDateTime reportDate;
    private int totalBookings;
    private Map<RoomBooking.BookingStatus, Long> statusBreakdown;
    private Map<String, Long> categoryBreakdown;
    private Map<String, Long> buildingBreakdown;
    private List<UserBookingAnalyticsResponse> topUsers;
    private List<RoomUtilizationAnalyticsResponse> roomUtilization;
    private double overallUtilizationRate;
    private double averageApprovalTime;
}