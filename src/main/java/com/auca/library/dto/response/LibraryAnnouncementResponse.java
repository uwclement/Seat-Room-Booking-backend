package com.auca.library.dto.response;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class LibraryAnnouncementResponse {
    private Long id;
    private String title;
    private String message;
    private LocalDateTime startDateTime;
    private LocalDateTime endDateTime;
    private boolean isUIVisible;
    private boolean isNotificationEnabled;
    private String createdByName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private boolean isActive;
}