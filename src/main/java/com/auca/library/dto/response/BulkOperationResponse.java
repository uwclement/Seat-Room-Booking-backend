package com.auca.library.dto.response;

import java.time.LocalDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BulkOperationResponse {
    private int successCount;
    private int failureCount;
    private List<String> errors;
    private String summary; // Overall summary message
    private LocalDateTime processedAt;
    private String operationType; // "APPROVAL", "REJECTION", "CANCELLATION"
    private List<Long> successfulBookingIds;
    private List<Long> failedBookingIds;
    
    // Constructor for backward compatibility
    public BulkOperationResponse(int successCount, int failureCount, List<String> errors) {
        this.successCount = successCount;
        this.failureCount = failureCount;
        this.errors = errors;
        this.processedAt = LocalDateTime.now();
    }
    
    // Constructor with summary
    public BulkOperationResponse(int successCount, int failureCount, List<String> errors, String summary) {
        this.successCount = successCount;
        this.failureCount = failureCount;
        this.errors = errors;
        this.summary = summary;
        this.processedAt = LocalDateTime.now();
    }
}