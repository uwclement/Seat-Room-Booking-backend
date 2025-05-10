package com.auca.library.dto.response;

import lombok.Data;

@Data
public class LibraryScheduleResponse {
    private Long id;
    private String dayOfWeek;
    private String openTime;
    private String closeTime;
}