package com.auca.library.dto.response;

import lombok.Getter;
import lombok.Setter;


@Getter
@Setter 
public class AdminBookingResponse {
    private Long id;
    private String title;
    private String description;
    private java.time.LocalDateTime startTime;
    private java.time.LocalDateTime endTime;
    private com.auca.library.model.RoomBooking.BookingStatus status;
    private java.time.LocalDateTime createdAt;
    private java.time.LocalDateTime updatedAt;
    private Boolean requiresApproval;
    private java.time.LocalDateTime approvedAt;
    private String rejectionReason;
    private java.time.LocalDateTime checkedInAt;
    private Boolean publicBooking;
    private Long durationHours;
    
    // User and room info
    private UserResponse user;
    private RoomResponse room;
    private UserResponse approvedBy;
    
    // Helper fields
    private String building;
    private String floor;
    private String roomCategory;
    private Integer participantCount;
    private Boolean overdue;
}