package com.auca.library.dto.response;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class LibraryStatusResponse {
    private boolean isOpen;
    private String currentHours;
    private String message;
    private String specialMessage;
    private LocalDateTime currentDateTime;
    private LocalDateTime nextStatusChange;
}