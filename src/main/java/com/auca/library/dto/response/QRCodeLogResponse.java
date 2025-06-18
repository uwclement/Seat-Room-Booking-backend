package com.auca.library.dto.response;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class QRCodeLogResponse {
    private Long id;
    private String resourceType;
    private Long resourceId;
    private Integer qrVersion;
    private String generatedBy;
    private LocalDateTime generatedAt;
    private boolean isCurrent;
    private String generationReason;
}