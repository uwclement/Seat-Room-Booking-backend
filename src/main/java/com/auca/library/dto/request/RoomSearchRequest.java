package com.auca.library.dto.request;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class RoomSearchRequest {
    private String keyword;
    private String category;
    private Integer minCapacity;
    private Integer maxCapacity;
    private String building;
    private String floor;
    private String department;
    private List<Long> equipmentIds;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
}