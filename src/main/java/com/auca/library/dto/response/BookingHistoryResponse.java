package com.auca.library.dto.response;

import java.util.List;

import lombok.Data;

@Data
public class BookingHistoryResponse {
   private List<RoomBookingResponse> bookings;
    private BookingStatistics statistics;
    private Integer totalBookings;
    private Integer currentPage;
    private Integer totalPages;
    
    @Data
    public static class BookingStatistics {
        private Integer totalBookings;
        private Integer completedBookings;
        private Integer cancelledBookings;
        private Integer noShowBookings;
        private Double averageBookingDuration;
        private Double averageActualUsage;
        private List<RoomResponse> mostUsedRooms;
        private Double noShowRate;
        private Double utilizationRate;
    } 
}
