package com.auca.library.dto.response;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
public class BookingExportResponse {
    private String exportFormat;
    private int totalRecords;
    private LocalDateTime exportDate;
    private String downloadUrl;
    private List<Map<String, Object>> data;
    private String fileName;
    private long fileSizeBytes;
}