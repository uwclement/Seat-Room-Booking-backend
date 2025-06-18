package com.auca.library.dto.response;

import lombok.Data;
import java.util.List;

@Data
public class QRCodeStatisticsResponse {
    private Long totalSeats;
    private Long seatsWithQRCode;
    private Long seatsWithoutQRCode;
    private Long totalRooms;
    private Long roomsWithQRCode;
    private Long roomsWithoutQRCode;
    private Long qrCodesGeneratedLastWeek;
    private List<QRCodeLogResponse> recentGenerations;
}