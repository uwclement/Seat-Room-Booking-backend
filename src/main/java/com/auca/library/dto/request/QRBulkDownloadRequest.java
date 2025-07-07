package com.auca.library.dto.request;

import lombok.Getter;
import lombok.Setter;
import java.util.List;

@Getter
@Setter
public class QRBulkDownloadRequest {
    private String type; // "SEAT" or "ROOM"
    private List<Long> resourceIds;
    private boolean downloadAll = false;

     public boolean isValid() {
        if (type == null || type.trim().isEmpty()) {
            return false;
        }
        
        if (!downloadAll && (resourceIds == null || resourceIds.isEmpty())) {
            return false;
        }
        
        return "SEAT".equalsIgnoreCase(type) || "ROOM".equalsIgnoreCase(type) ||
               "SEATS".equalsIgnoreCase(type) || "ROOMS".equalsIgnoreCase(type);
    }
}