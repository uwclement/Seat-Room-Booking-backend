package com.auca.library.dto.response;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
public class BulkQRGenerationResponse {
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer totalRequested;
    private Integer successCount;
    private Integer failureCount;
    private List<Long> successfulResourceIds;
    private List<String> errors;
    private Map<String, byte[]> generatedQRCodes; // For immediate download

    private boolean downloadAvailable;
    private String downloadMessage;
}