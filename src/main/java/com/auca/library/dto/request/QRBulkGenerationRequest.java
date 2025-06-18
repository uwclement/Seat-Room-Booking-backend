package com.auca.library.dto.request;

import lombok.Data;
import java.util.List;

@Data
public class QRBulkGenerationRequest {
    private List<Long> resourceIds; // Specific IDs to generate for
    private boolean generateForAll; // Generate for all resources
    private boolean generateForMissing; // Generate only for resources without QR codes
    private boolean regenerateExisting; // Regenerate even if QR code exists
    private boolean generateAndDownload; // Return as downloadable ZIP
}