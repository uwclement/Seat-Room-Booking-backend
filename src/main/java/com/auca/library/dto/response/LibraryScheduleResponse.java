package com.auca.library.dto.response;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class LibraryScheduleResponse {
    private Long id;
    private String dayOfWeek;
    private String openTime;
    private String closeTime;
    private Boolean isOpen;           
    private String specialCloseTime;  
    private String message;           
    private LocalDateTime lastModified; 
}