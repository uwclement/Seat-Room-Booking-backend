package com.auca.library.dto.response;

import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class LabClassAvailabilityResponse {
    private Long labClassId;
    private String labNumber;
    private String labName;
    private boolean available;
    
    // Current booking info
    private boolean currentlyBooked;
    private LocalDateTime currentBookingStart;
    private LocalDateTime currentBookingEnd;
    private String currentBookingUser;
    
    // Upcoming bookings
    private List<LabBookingSlot> upcomingBookings;
    
    // Next available slot
    private LocalDateTime nextAvailableSlot;
    private int hoursUntilAvailable;
    
    @Getter
    @Setter
    public static class LabBookingSlot {
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private String professorName;
        private String courseName;
        private List<String> equipmentNames;
    }
}