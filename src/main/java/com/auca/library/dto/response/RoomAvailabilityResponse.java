package com.auca.library.dto.response;

import java.time.LocalDateTime;
import java.util.List;

import lombok.Data;


@Data
public class RoomAvailabilityResponse {
        private RoomResponse room;
    private boolean isCurrentlyAvailable;
    private LocalDateTime nextAvailableTime;
    private LocalDateTime currentBookingEndTime;
    private List<TimeSlot> availableSlots;
    private List<BookingSlot> bookedSlots;


    @Data
    public static class TimeSlot {
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private boolean isRecommended;
        private String recommendationReason;
    }
    
    @Data
    public static class BookingSlot {
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private String bookedBy;
        private boolean isPrivate;
        private boolean canJoin;
        private Integer availableSpots;
    }
}
