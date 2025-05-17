package com.auca.library.dto.request;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class LibraryAnnouncementRequest {
    private String title;
    private String message;
    private LocalDateTime startDateTime;
    private LocalDateTime endDateTime;
    private boolean isUIVisible = true;
    private boolean isNotificationEnabled = false;
}