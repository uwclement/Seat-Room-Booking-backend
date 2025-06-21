package com.auca.library.dto.response;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class QRScanContext {
    private String token;
    private String type; 
    private String resourceIdentifier; 
    private LocalDateTime scannedAt;
    private String redirectUrl; 
}