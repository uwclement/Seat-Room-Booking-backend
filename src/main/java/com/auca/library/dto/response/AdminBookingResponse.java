package com.auca.library.dto.response;

import com.auca.library.model.RoomBooking;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class AdminBookingResponse {
    private Long id;
    private String title;
    private String description;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private RoomBooking.BookingStatus status;
    private UserResponse user;
    private RoomResponse room;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private boolean requiresApproval;
    private UserResponse approvedBy;
    private LocalDateTime approvedAt;
    private String rejectionReason;
    private LocalDateTime checkedInAt;
    private Long durationHours;
    private boolean overdue;
    private boolean publicBooking;
    private Integer participantCount;
    private String building;
    private String floor;
    private String roomCategory;
}