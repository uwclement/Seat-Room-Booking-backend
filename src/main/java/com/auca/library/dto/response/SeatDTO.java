package com.auca.library.dto.response;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class SeatDTO {
    private Long id;
    private String seatNumber;
    private String zoneType;
    private boolean hasDesktop;
    private String description;
    private String location;
    private Integer floar;
    private boolean isAvailable;
    private boolean isFavorite;
    private String nextAvailableTime; // For booked seats
    private int waitingCount; 
    private boolean disabled;


    private String qrCodeUrl;
    private String qrImageUrl;
    private boolean hasQRCode;
    private LocalDateTime qrGeneratedAt;
}
