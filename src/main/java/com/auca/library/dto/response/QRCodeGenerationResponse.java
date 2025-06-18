package com.auca.library.dto.response;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class QRCodeGenerationResponse {
    private boolean success;
    private String resourceType;
    private Long resourceId;
    private String resourceIdentifier;
    private String qrCodeUrl;
    private String qrCodeToken;
    private String imagePath;
    private LocalDateTime generatedAt;
    private Integer version;
    private String errorMessage;
}