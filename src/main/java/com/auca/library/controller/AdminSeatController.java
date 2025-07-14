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
import org.springframework.web.bind.annotation.RestController;

import com.auca.library.dto.request.BulkSeatUpdateRequest;
import com.auca.library.dto.request.QRBulkGenerationRequest;
import com.auca.library.dto.response.BulkQRGenerationResponse;
import com.auca.library.dto.response.MessageResponse;
import com.auca.library.dto.response.QRCodeGenerationResponse;
import com.auca.library.dto.response.SeatDTO;
import com.auca.library.service.AdminQRCodeService;
import com.auca.library.service.SeatService;

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

    // Get all seats (admin view)
    @GetMapping
    public ResponseEntity<List<SeatDTO>> getAllSeats() {
        return ResponseEntity.ok(seatService.getAllSeatsForAdmin());
    }

    // Create a new seat
    @PostMapping
    public ResponseEntity<SeatDTO> createSeat(@Valid @RequestBody SeatDTO seatDTO) {
        return ResponseEntity.ok(seatService.createSeat(seatDTO));
    }

    // Update a seat
    @PutMapping("/{id}")
    public ResponseEntity<SeatDTO> updateSeat(
            @PathVariable Long id,
            @Valid @RequestBody SeatDTO seatDTO) {
        return ResponseEntity.ok(seatService.updateSeat(id, seatDTO));
    }

    // Delete a seat
    @DeleteMapping("/{id}")
    public ResponseEntity<MessageResponse> deleteSeat(@PathVariable Long id) {
        seatService.deleteSeat(id);
        return ResponseEntity.ok(new MessageResponse("Seat deleted successfully"));
    }

    // Bulk update seats - main feature for admin
    @PutMapping("/bulk")
    public ResponseEntity<List<SeatDTO>> bulkUpdateSeats(
            @Valid @RequestBody BulkSeatUpdateRequest bulkUpdateRequest) {
        return ResponseEntity.ok(seatService.bulkUpdateSeats(bulkUpdateRequest));
    }

    // Toggle desktop property for a seat
    @PutMapping("/{id}/toggle-desktop")
    public ResponseEntity<SeatDTO> toggleDesktop(@PathVariable Long id) {
        return ResponseEntity.ok(seatService.toggleDesktopProperty(id));
    }

    // bulk toggle dektop
    @PutMapping("/bulk-toggle-desktop")
    public ResponseEntity<List<SeatDTO>> bulkToggleDesktop(@RequestBody Set<Long> seatIds) {
       return ResponseEntity.ok(seatService.bulkToggleDesktop(seatIds));
    }

    // Disable seats for maintenance
    @PutMapping("/disable")
    public ResponseEntity<List<SeatDTO>> disableSeats(@RequestBody Set<Long> seatIds) {
        return ResponseEntity.ok(seatService.disableSeats(seatIds, true));
    }

    // Enable seats after maintenance
    @PutMapping("/enable")
    public ResponseEntity<List<SeatDTO>> enableSeats(@RequestBody Set<Long> seatIds) {
        return ResponseEntity.ok(seatService.disableSeats(seatIds, false));
    }

    // Get all disabled seats
    @GetMapping("/disabled")
    public ResponseEntity<List<SeatDTO>> getDisabledSeats() {
        return ResponseEntity.ok(seatService.getDisabledSeats());
    }

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