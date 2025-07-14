package com.auca.library.dto.response;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BulkActionResponse {
    private boolean success;
    private String message;
    private int successCount;
    private int failureCount;
    private List<String> errors;
    private List<Long> processedUserIds;
}