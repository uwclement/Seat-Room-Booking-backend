package com.auca.library.dto.response;

import lombok.Data;

@Data
public class RoomDetailsResponse {
    private Long roomId;
    private String roomNumber;
    private String roomName;
    private String category;
    private Integer capacity;
    private String building;
    private String floor;
    private boolean requiresApproval;
}