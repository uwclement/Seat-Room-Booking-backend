package com.auca.library.dto.response;

import lombok.Data;

@Data
public class SeatDTO {
    private Long id;
    private String seatNumber;
    private String zoneType;
    private boolean hasDesktop;
    private String description;
    private boolean isAvailable;
    private boolean isFavorite;
    private String nextAvailableTime; // For booked seats
    private int waitingCount; // Number of people in the wait list
}
