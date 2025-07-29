package com.auca.library.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.auca.library.dto.request.QRBulkGenerationRequest;
import com.auca.library.dto.response.BulkQRGenerationResponse;
import com.auca.library.dto.response.QRCodeGenerationResponse;
import com.auca.library.dto.response.QRCodeLogResponse;
import com.auca.library.dto.response.QRCodeStatisticsResponse;
import com.auca.library.service.AdminQRCodeService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin/qr")
@CrossOrigin(origins = "*", maxAge = 3600)
@PreAuthorize("hasRole('ADMIN') or hasRole('LIBRARIAN') ")
public class AdminQRCodeController {

    @Autowired
    private AdminQRCodeService adminQRCodeService;


    // ========== SINGLE QR CODE GENERATION ==========

    @PostMapping("/generate/seat/{seatId}")
    @Operation(summary = "Generate QR code for a seat", description = "Generate or regenerate QR code for a specific seat")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "QR code generated successfully"),
        @ApiResponse(responseCode = "404", description = "Seat not found"),
        @ApiResponse(responseCode = "500", description = "Error generating QR code")
    })
    public ResponseEntity<QRCodeGenerationResponse> generateSeatQRCode(
            @Parameter(description = "Seat ID") @PathVariable Long seatId,
            Authentication authentication) {
        
        try {
            String adminEmail = authentication.getName();
            QRCodeGenerationResponse response = adminQRCodeService.generateSeatQRCode(seatId, adminEmail);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Failed to generate QR code: " + e.getMessage()));
        }
    }

    @PostMapping("/generate/room/{roomId}")
    @Operation(summary = "Generate QR code for a room", description = "Generate or regenerate QR code for a specific room")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "QR code generated successfully"),
        @ApiResponse(responseCode = "404", description = "Room not found"),
        @ApiResponse(responseCode = "500", description = "Error generating QR code")
    })
    public ResponseEntity<QRCodeGenerationResponse> generateRoomQRCode(
            @Parameter(description = "Room ID") @PathVariable Long roomId,
            Authentication authentication) {
        
        try {
            String adminEmail = authentication.getName();
            QRCodeGenerationResponse response = adminQRCodeService.generateRoomQRCode(roomId, adminEmail);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Failed to generate QR code: " + e.getMessage()));
        }
    }

    // ========== BULK QR CODE GENERATION ==========

    @PostMapping("/generate/bulk/seats")
    @Operation(summary = "Bulk generate QR codes for seats", 
               description = "Generate QR codes for multiple seats at once")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Bulk generation completed"),
        @ApiResponse(responseCode = "400", description = "Invalid request")
    })
    public ResponseEntity<?> bulkGenerateSeatQRCodes(
            @Valid @RequestBody QRBulkGenerationRequest request,
            Authentication authentication) {
        
        try {
            String adminEmail = authentication.getName();
            BulkQRGenerationResponse response = adminQRCodeService.bulkGenerateSeatQRCodes(request, adminEmail);
            
            // If generateAndDownload is true, return downloadable ZIP
            if (request.isGenerateAndDownload() && 
                !response.getGeneratedQRCodes().isEmpty()) {
                try {
                    return adminQRCodeService.downloadBulkQRCodes(response.getGeneratedQRCodes(), "SEATS");
                } catch (Exception e) {
                    // Fall back to regular response if download fails
                    response.setDownloadAvailable(false);
                    response.setDownloadMessage("QR codes generated but download failed: " + e.getMessage());
                }
            }
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Bulk generation failed: " + e.getMessage());
        }
    }

    @PostMapping("/generate/bulk/rooms")
    @Operation(summary = "Bulk generate QR codes for rooms", 
               description = "Generate QR codes for multiple rooms at once")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Bulk generation completed"),
        @ApiResponse(responseCode = "400", description = "Invalid request")
    })
    public ResponseEntity<?> bulkGenerateRoomQRCodes(
            @Valid @RequestBody QRBulkGenerationRequest request,
            Authentication authentication) {
        
        try {
            String adminEmail = authentication.getName();
            BulkQRGenerationResponse response = adminQRCodeService.bulkGenerateRoomQRCodes(request, adminEmail);
            
            // If generateAndDownload is true, return downloadable ZIP
            if (request.isGenerateAndDownload() && 
                !response.getGeneratedQRCodes().isEmpty()) {
                try {
                    return adminQRCodeService.downloadBulkQRCodes(response.getGeneratedQRCodes(), "ROOMS");
                } catch (Exception e) {
                    // Fall back to regular response if download fails
                    response.setDownloadAvailable(false);
                    response.setDownloadMessage("QR codes generated but download failed: " + e.getMessage());
                }
            }
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Bulk generation failed: " + e.getMessage());
        }
    }

    // ========== QR CODE DOWNLOAD ==========

    @GetMapping("/download/{type}/{resourceId}")
    @Operation(summary = "Download QR code image", 
               description = "Download the QR code image for a specific seat or room")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "QR code image returned"),
        @ApiResponse(responseCode = "404", description = "QR code not found")
    })
    public ResponseEntity<Resource> downloadQRCode(
            @Parameter(description = "Resource type (seat or room)") @PathVariable String type,
            @Parameter(description = "Resource ID") @PathVariable Long resourceId) {
        
        try {
            return adminQRCodeService.downloadQRCode(type, resourceId);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/download/bulk")
    @Operation(summary = "Download multiple QR codes as ZIP", 
               description = "Download selected QR codes in a ZIP file")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "ZIP file with QR codes"),
        @ApiResponse(responseCode = "400", description = "Invalid request"),
        @ApiResponse(responseCode = "404", description = "No QR codes found"),
        @ApiResponse(responseCode = "500", description = "Download failed")
    })
    public ResponseEntity<Resource> downloadBulkQRCodes(
            @Valid @RequestBody QRBulkDownloadRequest request) {
        
        try {
            // Validate request
            if (request.getType() == null || request.getType().trim().isEmpty()) {
                return ResponseEntity.badRequest().build();
            }
            
            if (!request.isDownloadAll() && 
                (request.getResourceIds() == null || request.getResourceIds().isEmpty())) {
                return ResponseEntity.badRequest().build();
            }
            
            return adminQRCodeService.downloadSelectedQRCodes(request);
            
        } catch (Exception e) {
            if (e.getMessage().contains("not found")) {
                return ResponseEntity.notFound().build();
            }
            System.err.println("Bulk download error: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ========== QR CODE STATISTICS AND HISTORY ==========

    @GetMapping("/statistics")
    @Operation(summary = "Get QR code statistics", 
               description = "Get statistics about QR code generation and coverage")
    public ResponseEntity<QRCodeStatisticsResponse> getQRCodeStatistics() {
        QRCodeStatisticsResponse statistics = adminQRCodeService.getQRCodeStatistics();
        return ResponseEntity.ok(statistics);
    }

    @GetMapping("/history")
    @Operation(summary = "Get QR generation history", 
               description = "Get history of QR code generations with filters")
    public ResponseEntity<List<QRCodeLogResponse>> getQRGenerationHistory(
            @Parameter(description = "Resource type filter") @RequestParam(required = false) String type,
            @Parameter(description = "Resource ID filter") @RequestParam(required = false) Long resourceId) {
        
        List<QRCodeLogResponse> history = adminQRCodeService.getQRGenerationHistory(type, resourceId);
        return ResponseEntity.ok(history);
    }

    // ========== QUICK ACTIONS ==========

    @PostMapping("/generate/all-missing")
    @Operation(summary = "Generate QR codes for all resources without QR codes", 
               description = "Quick action to generate QR codes for all seats and rooms that don't have one")
    public ResponseEntity<BulkQRGenerationSummaryResponse> generateAllMissingQRCodes(
            Authentication authentication) {
        
        String adminEmail = authentication.getName();
        
        // Generate for seats
        QRBulkGenerationRequest seatRequest = new QRBulkGenerationRequest();
        seatRequest.setGenerateForMissing(true);
        BulkQRGenerationResponse seatResponse = adminQRCodeService.bulkGenerateSeatQRCodes(seatRequest, adminEmail);
        
        // Generate for rooms
        QRBulkGenerationRequest roomRequest = new QRBulkGenerationRequest();
        roomRequest.setGenerateForMissing(true);
        BulkQRGenerationResponse roomResponse = adminQRCodeService.bulkGenerateRoomQRCodes(roomRequest, adminEmail);
        
        // Create summary response
        BulkQRGenerationSummaryResponse summary = new BulkQRGenerationSummaryResponse();
        summary.setSeatsGenerated(seatResponse.getSuccessCount());
        summary.setSeatsFailed(seatResponse.getFailureCount());
        summary.setRoomsGenerated(roomResponse.getSuccessCount());
        summary.setRoomsFailed(roomResponse.getFailureCount());
        summary.setTotalGenerated(seatResponse.getSuccessCount() + roomResponse.getSuccessCount());
        summary.setTotalFailed(seatResponse.getFailureCount() + roomResponse.getFailureCount());
        
        return ResponseEntity.ok(summary);
    }

    // ========== HELPER METHODS ==========

    private QRCodeGenerationResponse createErrorResponse(String message) {
        QRCodeGenerationResponse response = new QRCodeGenerationResponse();
        response.setSuccess(false);
        response.setErrorMessage(message);
        return response;
    }

    // ========== REQUEST/RESPONSE CLASSES ==========

    public static class QRBulkDownloadRequest {
        private String type; // SEATS or ROOMS
        private List<Long> resourceIds;
        private boolean downloadAll;
        
        // Getters and setters
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public List<Long> getResourceIds() { return resourceIds; }
        public void setResourceIds(List<Long> resourceIds) { this.resourceIds = resourceIds; }
        public boolean isDownloadAll() { return downloadAll; }
        public void setDownloadAll(boolean downloadAll) { this.downloadAll = downloadAll; }
    }

    public static class BulkQRGenerationSummaryResponse {
        private int seatsGenerated;
        private int seatsFailed;
        private int roomsGenerated;
        private int roomsFailed;
        private int totalGenerated;
        private int totalFailed;
        
        // Getters and setters
        public int getSeatsGenerated() { return seatsGenerated; }
        public void setSeatsGenerated(int seatsGenerated) { this.seatsGenerated = seatsGenerated; }
        public int getSeatsFailed() { return seatsFailed; }
        public void setSeatsFailed(int seatsFailed) { this.seatsFailed = seatsFailed; }
        public int getRoomsGenerated() { return roomsGenerated; }
        public void setRoomsGenerated(int roomsGenerated) { this.roomsGenerated = roomsGenerated; }
        public int getRoomsFailed() { return roomsFailed; }
        public void setRoomsFailed(int roomsFailed) { this.roomsFailed = roomsFailed; }
        public int getTotalGenerated() { return totalGenerated; }
        public void setTotalGenerated(int totalGenerated) { this.totalGenerated = totalGenerated; }
        public int getTotalFailed() { return totalFailed; }
        public void setTotalFailed(int totalFailed) { this.totalFailed = totalFailed; }
    }
}