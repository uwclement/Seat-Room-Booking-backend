package com.auca.library.dto.response;

import java.time.LocalDateTime;

import com.auca.library.model.LabRequest;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LabRequestResponse {
    private Long id;
    private Long labClassId;
    private String labClassName;
    private String labNumber;
    private Long courseId;
    private String courseCode;
    private String courseName;
    private String reason;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private LabRequest.RequestStatus status;
    private String rejectionReason;
    private String adminSuggestion;
    private LocalDateTime createdAt;
    private LocalDateTime approvedAt;
    private boolean escalatedToHod;
    private LocalDateTime escalatedAt;
    private Long userId;
    private String userFullName;
    private String approvedByName;
    private Long durationHours;
}