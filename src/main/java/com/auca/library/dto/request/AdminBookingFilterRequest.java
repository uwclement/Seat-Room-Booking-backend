package com.auca.library.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminBookingFilterRequest {
    private String status;
    private Long roomId;
    private String roomCategory;
    private String building;
    private String floor;
    private Long userId;
    private String userEmail;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private Integer minDurationHours;
    private Integer maxDurationHours;
    private Boolean publicOnly;
    private Boolean requiresApproval;
    private Long approvedBy;
    private String search;
    private Boolean overdue;
    private Boolean checkedIn;
    private Boolean recurring;
}