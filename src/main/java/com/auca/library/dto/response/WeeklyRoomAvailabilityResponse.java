package com.auca.library.dto.response;

import java.time.LocalDateTime;
import java.util.List;

import lombok.Data;

@Data
public class WeeklyRoomAvailabilityResponse {
     private RoomResponse room;
    private LocalDateTime weekStart;
    private LocalDateTime weekEnd;
    private List<DayAvailability> dailyAvailability;
    
    @Data
    public static class DayAvailability {
        private String dayOfWeek;
        private LocalDateTime date;
        private List<TimeSlot> availableSlots;
        private List<BookingSlot> bookedSlots;
        private boolean isLibraryOpen;
        private String libraryHours;
    }
    
    @Data
    public static class TimeSlot {
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private boolean isRecommended;
        private Integer maxDuration; // In minutes
    }
    
    @Data
    public static class BookingSlot {
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private String title;
        private boolean isPrivate;
        private boolean canJoin;
        private Integer availableSpots;
    } 
}
