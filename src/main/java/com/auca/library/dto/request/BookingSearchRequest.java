package com.auca.library.dto.request;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class BookingSearchRequest {
    private String keyword;
    private Long roomId;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private String status;
    private boolean publicOnly = false;
    private Long userId; // For admin searches
    private String building;
    private String floor;
    private Integer minCapacity;
    private Integer maxCapacity;
}