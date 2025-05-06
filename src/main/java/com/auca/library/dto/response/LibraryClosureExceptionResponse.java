package com.auca.library.dto.response;

import lombok.Data;

@Data
public class LibraryClosureExceptionResponse {
    private Long id;
    private String date;
    private String openTime;
    private String closeTime;
    private Boolean closedAllDay;
    private String reason;
}