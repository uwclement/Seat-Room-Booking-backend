package com.auca.library.dto.response;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class RoomBookingDetailsResponse {
    private Long bookingId;
    private String title;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String status;
    private String organizerName;
    private Integer totalParticipants;
    private Integer checkedInCount;
}