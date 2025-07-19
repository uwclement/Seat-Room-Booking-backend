package com.auca.library.controller;

import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.auca.library.dto.request.BulkSeatCreationRequest;
import com.auca.library.dto.request.BulkSeatUpdateRequest;
import com.auca.library.dto.request.QRBulkGenerationRequest;
import com.auca.library.dto.response.BulkQRGenerationResponse;
import com.auca.library.dto.response.MessageResponse;
import com.auca.library.dto.response.QRCodeGenerationResponse;
import com.auca.library.dto.response.SeatDTO;
import com.auca.library.exception.ResourceNotFoundException;
import com.auca.library.model.Location;
import com.auca.library.model.User;
import com.auca.library.service.AdminQRCodeService;
import com.auca.library.service.SeatService;
import com.auca.library.repository.UserRepository;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/admin/seats")
@PreAuthorize("hasRole('ADMIN')  or hasRole('LIBRARIAN') ")
public class AdminSeatController {

    @Autowired
    private SeatService seatService;

    @Autowired
    private AdminQRCodeService adminQRCodeService;

    @Autowired
    private UserRepository userRepository;

    // Get all seats (admin view)
@GetMapping
    @Operation(summary = "Get all seats", description = "Get all seats with optional location filtering")
    public ResponseEntity<List<SeatDTO>> getAllSeats(
            @RequestParam(required = false) Location location,
            Authentication authentication) {
        
        Location userLocation = getCurrentUserLocation(authentication);
        Location effectiveLocation = userLocation != null ? userLocation : location;
        
        List<SeatDTO> seats = seatService.getAllSeatsForAdmin(effectiveLocation);
        return ResponseEntity.ok(seats);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get seat by ID", description = "Get a specific seat by ID")
    public ResponseEntity<SeatDTO> getSeatById(
            @PathVariable Long id,
            Authentication authentication) {
        
        Location userLocation = getCurrentUserLocation(authentication);
        SeatDTO seat = seatService.getSeatById(id, userLocation);
        return ResponseEntity.ok(seat);
    }

    @GetMapping("/disabled")
    @Operation(summary = "Get disabled seats", description = "Get all disabled seats with optional location filtering")
    public ResponseEntity<List<SeatDTO>> getDisabledSeats(
            @RequestParam(required = false) Location location,
            Authentication authentication) {
        
        Location userLocation = getCurrentUserLocation(authentication);
        Location effectiveLocation = userLocation != null ? userLocation : location;
        
        List<SeatDTO> disabledSeats = seatService.getDisabledSeats(effectiveLocation);
        return ResponseEntity.ok(disabledSeats);
    }

    // ================== CREATE OPERATIONS ==================

    @PostMapping
    @Operation(summary = "Create a new seat", description = "Create a single new seat")
    public ResponseEntity<SeatDTO> createSeat(
            @Valid @RequestBody SeatDTO seatDTO,
            Authentication authentication) {
        
        Location userLocation = getCurrentUserLocation(authentication);
        SeatDTO createdSeat = seatService.createSeat(seatDTO, userLocation);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdSeat);
    }

    @PostMapping("/bulk-create")
    @Operation(summary = "Bulk create seats", description = "Create multiple seats at once")
    public ResponseEntity<List<SeatDTO>> bulkCreateSeats(
            @Valid @RequestBody BulkSeatCreationRequest request,
            Authentication authentication) {
        
        try {
            Location userLocation = getCurrentUserLocation(authentication);
            List<SeatDTO> createdSeats = seatService.bulkCreateSeats(request, userLocation);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdSeats);
        } catch (Exception e) {
            throw new IllegalArgumentException("Bulk seat creation failed: " + e.getMessage());
        }
    }

    // ================== UPDATE OPERATIONS ==================

    @PutMapping("/{id}")
    @Operation(summary = "Update a seat", description = "Update a specific seat")
    public ResponseEntity<SeatDTO> updateSeat(
            @PathVariable Long id,
            @Valid @RequestBody SeatDTO seatDTO,
            Authentication authentication) {
        
        Location userLocation = getCurrentUserLocation(authentication);
        SeatDTO updatedSeat = seatService.updateSeat(id, seatDTO, userLocation);
        return ResponseEntity.ok(updatedSeat);
    }

    @PutMapping("/bulk")
    @Operation(summary = "Bulk update seats", description = "Update multiple seats at once")
    public ResponseEntity<List<SeatDTO>> bulkUpdateSeats(
            @Valid @RequestBody BulkSeatUpdateRequest bulkUpdateRequest,
            Authentication authentication) {
        
        Location userLocation = getCurrentUserLocation(authentication);
        List<SeatDTO> updatedSeats = seatService.bulkUpdateSeats(bulkUpdateRequest, userLocation);
        return ResponseEntity.ok(updatedSeats);
    }

    @PutMapping("/{id}/toggle-desktop")
    @Operation(summary = "Toggle desktop property", description = "Toggle desktop property for a specific seat")
    public ResponseEntity<SeatDTO> toggleDesktop(
            @PathVariable Long id,
            Authentication authentication) {
        
        Location userLocation = getCurrentUserLocation(authentication);
        SeatDTO updatedSeat = seatService.toggleDesktopProperty(id, userLocation);
        return ResponseEntity.ok(updatedSeat);
    }

    @PutMapping("/bulk-toggle-desktop")
    @Operation(summary = "Bulk toggle desktop", description = "Toggle desktop property for multiple seats")
    public ResponseEntity<List<SeatDTO>> bulkToggleDesktop(
            @RequestBody Set<Long> seatIds,
            Authentication authentication) {
        
        Location userLocation = getCurrentUserLocation(authentication);
        List<SeatDTO> updatedSeats = seatService.bulkToggleDesktop(seatIds, userLocation);
        return ResponseEntity.ok(updatedSeats);
    }

    @PutMapping("/disable")
    @Operation(summary = "Disable seats", description = "Disable seats for maintenance")
    public ResponseEntity<List<SeatDTO>> disableSeats(
            @RequestBody Set<Long> seatIds,
            Authentication authentication) {
        
        Location userLocation = getCurrentUserLocation(authentication);
        List<SeatDTO> disabledSeats = seatService.disableSeats(seatIds, true, userLocation);
        return ResponseEntity.ok(disabledSeats);
    }

    @PutMapping("/enable")
    @Operation(summary = "Enable seats", description = "Enable seats after maintenance")
    public ResponseEntity<List<SeatDTO>> enableSeats(
            @RequestBody Set<Long> seatIds,
            Authentication authentication) {
        
        Location userLocation = getCurrentUserLocation(authentication);
        List<SeatDTO> enabledSeats = seatService.disableSeats(seatIds, false, userLocation);
        return ResponseEntity.ok(enabledSeats);
    }

    // ================== DELETE OPERATIONS ==================

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a seat", description = "Delete a specific seat")
    public ResponseEntity<MessageResponse> deleteSeat(
            @PathVariable Long id,
            Authentication authentication) {
        
        Location userLocation = getCurrentUserLocation(authentication);
        seatService.deleteSeat(id, userLocation);
        return ResponseEntity.ok(new MessageResponse("Seat deleted successfully"));
    }


     private Location getCurrentUserLocation(Authentication authentication) {
        String currentUserEmail = authentication.getName();
        User user = userRepository.findByEmail(currentUserEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        return user.isLibrarian() ? user.getLocation() : null;
    }

    // QR Operation 
@PostMapping("/{id}/generate-qr")
@Operation(summary = "Generate QR code for seat", description = "Generate or regenerate QR code for a specific seat")
public ResponseEntity<QRCodeGenerationResponse> generateSeatQR(@PathVariable Long id, 
       Authentication authentication) {

    try {
        String adminEmail = authentication.getName();
        QRCodeGenerationResponse response = adminQRCodeService.generateSeatQRCode(id, adminEmail);
        return ResponseEntity.ok(response);
    } catch (Exception e) {
        // Create proper QRCodeGenerationResponse for error case
        QRCodeGenerationResponse errorResponse = new QRCodeGenerationResponse();
        errorResponse.setSuccess(false);
        errorResponse.setErrorMessage("Failed to generate QR code: " + e.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
}

// // Fixed helper method - change return type to match usage
// private QRCodeGenerationResponse createQRErrorResponse(String message) {
//     QRCodeGenerationResponse errorResponse = new QRCodeGenerationResponse();
//     errorResponse.setSuccess(false);
//     errorResponse.setErrorMessage(message);
//     // errorResponse.setTimestamp(new Date().toString());
//     return errorResponse;
// }

      @PostMapping("/bulk-generate-qr")
      @Operation(summary = "Bulk generate QR codes for seats", description = "Generate QR codes for multiple seats")
       public ResponseEntity<BulkQRGenerationResponse> bulkGenerateSeatQRs( @Valid @RequestBody QRBulkGenerationRequest request,
        Authentication authentication) {
    
    String adminEmail = authentication.getName();
    BulkQRGenerationResponse response = adminQRCodeService.bulkGenerateSeatQRCodes(request, adminEmail);
    return ResponseEntity.ok(response);
}



}