package com.auca.library.dto.response;

import lombok.Data;

@Data
public class SeatDetailsResponse {
    private Long seatId;
    private String seatNumber;
    private String zoneType;
    private boolean hasDesktop;
    private String description;
    private String location;
    private Integer floar;
}