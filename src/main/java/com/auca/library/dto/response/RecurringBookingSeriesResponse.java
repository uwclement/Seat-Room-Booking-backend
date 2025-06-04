package com.auca.library.dto.response;

import java.time.LocalDateTime;
import java.util.Set;

import lombok.Data;

@Data
public class RecurringBookingSeriesResponse {
    private Long id;
    private UserResponse user;
    private RoomResponse room;
    private String title;
    private String description;
    private RecurrenceType recurrenceType;
    private Integer recurrenceInterval;
    private Set<String> daysOfWeek; // String representation for frontend
    private String startTime; // Time only
    private String endTime; // Time only
    private LocalDateTime seriesStartDate;
    private LocalDateTime seriesEndDate;
    private boolean isActive;
    private Integer totalBookings;
    private Integer completedBookings;
    private LocalDateTime lastGeneratedDate;
    private LocalDateTime createdAt;
    
    public enum RecurrenceType {
        DAILY, WEEKLY, MONTHLY, CUSTOM
    }
}
