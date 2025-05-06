package com.auca.library.controller;

import com.auca.library.dto.request.BulkSeatUpdateRequest;
import com.auca.library.dto.response.MessageResponse;
import com.auca.library.dto.response.SeatDTO;
import com.auca.library.service.SeatService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/admin/seats")
@PreAuthorize("hasRole('ADMIN')")
public class AdminSeatController {

    @Autowired
    private SeatService seatService;

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
}