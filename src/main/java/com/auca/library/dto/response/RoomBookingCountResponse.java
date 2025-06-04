package com.auca.library.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RoomBookingCountResponse {
    private RoomResponse room;
    private Long bookingCount;
}