package com.auca.library.controller;

import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.auca.library.dto.response.MessageResponse;
import com.auca.library.dto.response.QRScanResponse;
import com.auca.library.model.Booking;
import com.auca.library.model.BookingParticipant;
import com.auca.library.model.RoomBooking;
import com.auca.library.model.User;
import com.auca.library.repository.BookingParticipantRepository;
import com.auca.library.repository.BookingRepository;
import com.auca.library.repository.RoomBookingRepository;
import com.auca.library.repository.UserRepository;
import com.auca.library.service.BookingService;
import com.auca.library.service.QRScanService;
import com.auca.library.service.RoomBookingService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("api/scan")
@CrossOrigin(origins = "*", maxAge = 3600)
@Tag(name = "QR Code Scanning", description = "Public endpoints for QR code scanning")
public class QRScanController {

    @Autowired
    private QRScanService qrScanService;

    @Autowired
    private BookingService bookingService;

    @Autowired
    private RoomBookingService roomBookingService;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private RoomBookingRepository roomBookingRepository;

    @Autowired
    private BookingParticipantRepository participantRepository;

    @Autowired
    private UserRepository userRepository;

    /**
     * Public QR code scan endpoint - accessible without authentication
     */
    @GetMapping
    @Operation(
        summary = "Scan QR code", 
        description = "Process QR code scan from seat or room. Returns appropriate response based on user authentication and booking status"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Scan processed successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid QR code"),
        @ApiResponse(responseCode = "404", description = "Resource not found")
    })
    public ResponseEntity<QRScanResponse> scanQRCode(
            @Parameter(description = "Resource type (seat or room)", required = true) 
            @RequestParam String type,
            @Parameter(description = "QR code token", required = true) 
            @RequestParam String token,
            Authentication authentication) {
        
        // Get user email if authenticated (can be null for public scans)
        String userEmail = authentication != null ? authentication.getName() : null;
        
        try {
            // Process scan
            QRScanResponse response = qrScanService.processScan(type, token, userEmail);
            
            // Log scan (with better error handling)
            logScanSafely(type, token, userEmail, response.isSuccess());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            // Log error but don't fail the main operation
            logScanSafely(type, token, userEmail, false);
            
            // Return user-friendly error response
            QRScanResponse errorResponse = new QRScanResponse();
            errorResponse.setSuccess(false);
            errorResponse.setMessage("Failed to process QR code. Please try again.");
            errorResponse.setAction("ERROR");
            errorResponse.setErrorCode("PROCESSING_ERROR");
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Process check-in after QR scan (requires authentication)
     */
    @PostMapping("/checkin")
    @Operation(
        summary = "Check in via QR code", 
        description = "Process check-in for a booking after QR code scan"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Check-in successful"),
        @ApiResponse(responseCode = "400", description = "Invalid check-in request"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "404", description = "Booking not found")
    })
    public ResponseEntity<MessageResponse> processCheckIn(
            @Parameter(description = "Resource type (seat or room)", required = true) 
            @RequestParam String type,
            @Parameter(description = "Booking ID", required = true) 
            @RequestParam Long bookingId,
            @Parameter(description = "Participant ID (for room participants)") 
            @RequestParam(required = false) Long participantId,
            Authentication authentication) {
        
        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new MessageResponse("Authentication required for check-in"));
        }
        
        String userEmail = authentication.getName();
        
        try {
            if ("seat".equalsIgnoreCase(type)) {
                return processSeatCheckIn(bookingId, userEmail);
            } else if ("room".equalsIgnoreCase(type)) {
                return processRoomCheckIn(bookingId, participantId, userEmail);
            } else {
                return ResponseEntity.badRequest()
                    .body(new MessageResponse("Invalid resource type"));
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(new MessageResponse("Check-in failed: " + e.getMessage()));
        }
    }

    /**
     * Process stored QR scan after login
     */
    @PostMapping("/process-stored")
    @Operation(
        summary = "Process stored QR scan after login", 
        description = "Validates QR code against user's current booking for in-app check-in"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Stored scan processed"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<QRScanResponse> processStoredScan(
            @RequestBody QRScanContext scanContext,
            Authentication authentication) {
        
        if (authentication == null) {
            QRScanResponse errorResponse = new QRScanResponse();
            errorResponse.setSuccess(false);
            errorResponse.setRequiresAuthentication(true);
            errorResponse.setMessage("Authentication required");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
        }
        
        String userEmail = authentication.getName();
        
        try {
            // Process the stored scan with user context
            QRScanResponse response = qrScanService.processScan(
                scanContext.getType(), 
                scanContext.getToken(), 
                userEmail
            );
            
            // Log the completion of stored scan
            logScanSafely(scanContext.getType(), scanContext.getToken(), userEmail, response.isSuccess());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logScanSafely(scanContext.getType(), scanContext.getToken(), userEmail, false);
            
            QRScanResponse errorResponse = new QRScanResponse();
            errorResponse.setSuccess(false);
            errorResponse.setMessage("Failed to process stored scan");
            errorResponse.setAction("ERROR");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

        /**
     * NEW: Safe logging method that doesn't cause transaction rollback
     */
    private void logScanSafely(String type, String token, String userEmail, boolean success) {
        try {
            // Use a separate transaction for logging to avoid rollback issues
            qrScanService.logScanAsync(type, token, userEmail, success);
        } catch (Exception e) {
            // Log to console but don't fail the main operation
            System.err.println("Failed to log QR scan: " + e.getMessage());
        }
    }

    /**
 * Mobile app QR validation endpoint
 */
@PostMapping("/validate")
@Operation(
    summary = "Validate QR code for mobile check-in", 
    description = "Validates QR code against user's current booking for in-app check-in"
)
@ApiResponses(value = {
    @ApiResponse(responseCode = "200", description = "Validation result"),
    @ApiResponse(responseCode = "401", description = "Unauthorized")
})
public ResponseEntity<QRValidationResponse> validateQRForCheckIn(
        @RequestBody QRValidationRequest request,
        Authentication authentication) {
    
    if (authentication == null) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
    
    String userEmail = authentication.getName();
    QRValidationResponse response = new QRValidationResponse();
    
    // Extract token from scanned QR content
    String token = extractTokenFromQRContent(request.getQrContent());
    if (token == null) {
        response.setValid(false);
        response.setMessage("Invalid QR code format");
        return ResponseEntity.ok(response);
    }
    
    // Determine resource type from QR content
    String type = extractTypeFromQRContent(request.getQrContent());
    
    // Get scan response
    QRScanResponse scanResponse = qrScanService.processScan(type, token, userEmail);
    
    // Check if this matches the expected booking
    if (request.getExpectedBookingId() != null) {
        Long bookingId = extractBookingId(scanResponse.getBookingDetails());
        if (bookingId != null && bookingId.equals(request.getExpectedBookingId())) {
            response.setValid(true);
            response.setMessage("QR code matches your booking");
            response.setCanCheckIn(scanResponse.isCanCheckIn());
            response.setBookingId(request.getExpectedBookingId());
        } else {
            response.setValid(false);
            response.setMessage("This QR code doesn't match your current booking");
        }
    } else {
        // No specific booking expected, return general validation
        response.setValid(scanResponse.isSuccess());
        response.setMessage(scanResponse.getMessage());
        response.setCanCheckIn(scanResponse.isCanCheckIn());
        Long bookingId = extractBookingId(scanResponse.getBookingDetails());
        if (bookingId != null) {
            response.setBookingId(bookingId);
        }
    }
    
    return ResponseEntity.ok(response);
}

// Helper method to safely extract booking ID from booking details
private Long extractBookingId(Object bookingDetails) {
    if (bookingDetails == null) {
        return null;
    }
    
    // Handle different types of booking details
    try {
        // Try to get bookingId using reflection
        java.lang.reflect.Method getBookingIdMethod = bookingDetails.getClass().getMethod("getBookingId");
        return (Long) getBookingIdMethod.invoke(bookingDetails);
    } catch (Exception e) {
        // If reflection fails, return null
        return null;
    }
}

    /**
     * Get QR scan info without authentication (for display purposes)
     */
    @GetMapping("/info")
    @Operation(
        summary = "Get QR code information", 
        description = "Get basic information about a QR code without authentication"
    )
    public ResponseEntity<QRInfoResponse> getQRInfo(
            @Parameter(description = "Resource type (seat or room)", required = true) 
            @RequestParam String type,
            @Parameter(description = "QR code token", required = true) 
            @RequestParam String token) {
        
        // Process scan without user context
        QRScanResponse scanResponse = qrScanService.processScan(type, token, null);
        
        QRInfoResponse infoResponse = new QRInfoResponse();
        infoResponse.setValid(scanResponse.isSuccess());
        infoResponse.setResourceType(scanResponse.getResourceType());
        infoResponse.setResourceIdentifier(scanResponse.getResourceIdentifier());
        infoResponse.setResourceDetails(scanResponse.getResourceDetails());
        infoResponse.setAvailabilityInfo(scanResponse.getAvailabilityInfo());
        
        if (!scanResponse.isSuccess()) {
            infoResponse.setErrorMessage(scanResponse.getMessage());
        }
        
        return ResponseEntity.ok(infoResponse);
    }

    // Private helper methods

    private ResponseEntity<MessageResponse> processSeatCheckIn(Long bookingId, String userEmail) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found"));
        
        // Verify booking belongs to user
        if (!booking.getUser().getEmail().equals(userEmail)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new MessageResponse("This booking does not belong to you"));
        }
        
        // Check if already checked in
        if (booking.getStatus() == Booking.BookingStatus.CHECKED_IN) {
            return ResponseEntity.ok(new MessageResponse("Already checked in"));
        }
        
        // Perform check-in
        bookingService.checkIn(bookingId);
        
        return ResponseEntity.ok(new MessageResponse("Check-in successful"));
    }

   private ResponseEntity<MessageResponse> processRoomCheckIn(Long bookingId, Long participantId, String userEmail) {
    RoomBooking booking = roomBookingRepository.findById(bookingId)
            .orElseThrow(() -> new RuntimeException("Booking not found"));
    
    User user = userRepository.findByEmail(userEmail)
            .orElseThrow(() -> new RuntimeException("User not found"));
    
    // NEW: Verify we're still within check-in window
    LocalDateTime now = LocalDateTime.now();
    LocalDateTime checkInStart = booking.getStartTime().minusMinutes(10);
    LocalDateTime checkInEnd = booking.getStartTime().plusMinutes(10);
    
    if (now.isBefore(checkInStart) || now.isAfter(checkInEnd)) {
        return ResponseEntity.badRequest()
            .body(new MessageResponse("Check-in window has expired"));
    }
    
    // Check if user is organizer
    if (booking.getUser().getEmail().equals(userEmail)) {
        // Organizer check-in
        if (booking.getCheckedInAt() != null) {
            return ResponseEntity.ok(new MessageResponse("Already checked in"));
        }
        
        booking.setCheckedInAt(LocalDateTime.now());
        booking.setStatus(RoomBooking.BookingStatus.CHECKED_IN);
        roomBookingRepository.save(booking);
        
        return ResponseEntity.ok(new MessageResponse("Check-in successful"));
    }
    
    // Participant check-in
    if (participantId == null) {
        return ResponseEntity.badRequest()
            .body(new MessageResponse("Participant ID required for participant check-in"));
    }
    
    BookingParticipant participant = participantRepository.findById(participantId)
            .orElseThrow(() -> new RuntimeException("Participant record not found"));
    
    // Verify participant belongs to user
    if (!participant.getUser().getEmail().equals(userEmail)) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
            .body(new MessageResponse("This participant record does not belong to you"));
    }
    
    // Check if already checked in
    if (participant.getCheckedInAt() != null) {
        return ResponseEntity.ok(new MessageResponse("Already checked in as participant"));
    }
    
    // Perform participant check-in
    participant.setCheckedInAt(LocalDateTime.now());
    participantRepository.save(participant);
    
    return ResponseEntity.ok(new MessageResponse("Participant check-in successful"));
}

    private String extractTokenFromQRContent(String qrContent) {
        if (qrContent == null || qrContent.isEmpty()) {
            return null;
        }
        
        // Expected format: https://library.edu/scan?type=seat&token=uuid
        String tokenParam = "token=";
        int tokenIndex = qrContent.indexOf(tokenParam);
        if (tokenIndex == -1) {
            return null;
        }
        
        int tokenStart = tokenIndex + tokenParam.length();
        int tokenEnd = qrContent.indexOf('&', tokenStart);
        if (tokenEnd == -1) {
            tokenEnd = qrContent.length();
        }
        
        return qrContent.substring(tokenStart, tokenEnd);
    }


    private String extractTypeFromQRContent(String qrContent) {
        if (qrContent == null || qrContent.isEmpty()) {
            return null;
        }
        
        // Expected format: https://library.edu/scan?type=seat&token=uuid
        String typeParam = "type=";
        int typeIndex = qrContent.indexOf(typeParam);
        if (typeIndex == -1) {
            return null;
        }
        
        int typeStart = typeIndex + typeParam.length();
        int typeEnd = qrContent.indexOf('&', typeStart);
        if (typeEnd == -1) {
            typeEnd = qrContent.length();
        }
        
        return qrContent.substring(typeStart, typeEnd);
    }

    // Inner classes for request/response DTOs
    
     public static class QRScanContext {
        private String token;
        private String type;
        private String resourceIdentifier;
        private String scannedAt;
        
        // Getters and setters
        public String getToken() { return token; }
        public void setToken(String token) { this.token = token; }
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public String getResourceIdentifier() { return resourceIdentifier; }
        public void setResourceIdentifier(String resourceIdentifier) { this.resourceIdentifier = resourceIdentifier; }
        public String getScannedAt() { return scannedAt; }
        public void setScannedAt(String scannedAt) { this.scannedAt = scannedAt; }
    }
    public static class QRValidationRequest {
        private String qrContent;
        private Long expectedBookingId;
        
        // Getters and setters
        public String getQrContent() { return qrContent; }
        public void setQrContent(String qrContent) { this.qrContent = qrContent; }
        public Long getExpectedBookingId() { return expectedBookingId; }
        public void setExpectedBookingId(Long expectedBookingId) { this.expectedBookingId = expectedBookingId; }
    }
    
    public static class QRValidationResponse {
        private boolean valid;
        private String message;
        private boolean canCheckIn;
        private Long bookingId;
        
        // Getters and setters
        public boolean isValid() { return valid; }
        public void setValid(boolean valid) { this.valid = valid; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public boolean isCanCheckIn() { return canCheckIn; }
        public void setCanCheckIn(boolean canCheckIn) { this.canCheckIn = canCheckIn; }
        public Long getBookingId() { return bookingId; }
        public void setBookingId(Long bookingId) { this.bookingId = bookingId; }
    }
    
    public static class QRInfoResponse {
        private boolean valid;
        private String resourceType;
        private String resourceIdentifier;
        private Object resourceDetails;
        private String availabilityInfo;
        private String errorMessage;
        
        // Getters and setters
        public boolean isValid() { return valid; }
        public void setValid(boolean valid) { this.valid = valid; }
        public String getResourceType() { return resourceType; }
        public void setResourceType(String resourceType) { this.resourceType = resourceType; }
        public String getResourceIdentifier() { return resourceIdentifier; }
        public void setResourceIdentifier(String resourceIdentifier) { this.resourceIdentifier = resourceIdentifier; }
        public Object getResourceDetails() { return resourceDetails; }
        public void setResourceDetails(Object resourceDetails) { this.resourceDetails = resourceDetails; }
        public String getAvailabilityInfo() { return availabilityInfo; }
        public void setAvailabilityInfo(String availabilityInfo) { this.availabilityInfo = availabilityInfo; }
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    }
}