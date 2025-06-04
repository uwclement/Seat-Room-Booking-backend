package com.auca.library.dto.response;

import com.auca.library.model.RoomBooking;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class BookingApprovalHistoryResponse {
    private Long bookingId;
    private String title;
    private UserResponse user;
    private RoomResponse room;
    private RoomBooking.BookingStatus status;
    private UserResponse approvedBy;
    private LocalDateTime approvedAt;
    private String rejectionReason;
    private LocalDateTime createdAt;
    private Long approvalTimeHours;
    private String action; // APPROVED, REJECTED
}