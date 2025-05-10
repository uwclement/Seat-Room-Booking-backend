package com.auca.library.controller;

import com.auca.library.dto.request.SeatAvailabilityRequest;
import com.auca.library.dto.response.MessageResponse;
import com.auca.library.dto.response.SeatDTO;
import com.auca.library.service.SeatService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/seats")
public class SeatController {

    @Autowired
    private SeatService seatService;
    
    @GetMapping
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<List<SeatDTO>> getAllSeats() {
        List<SeatDTO> seats = seatService.getAllSeats();
        return ResponseEntity.ok(seats);
    }
    
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<SeatDTO> getSeatById(@PathVariable Long id) {
        SeatDTO seat = seatService.getSeatById(id);
        return ResponseEntity.ok(seat);
    }
    
    @PostMapping("/available")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<List<SeatDTO>> getAvailableSeats(@Valid @RequestBody SeatAvailabilityRequest request) {
        List<SeatDTO> seats = seatService.getAvailableSeats(request);
        return ResponseEntity.ok(seats);
    }
    
    @PostMapping("/{id}/favorite")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<MessageResponse> toggleFavoriteSeat(@PathVariable Long id) {
        boolean isFavorite = seatService.toggleFavoriteSeat(id);
        String message = isFavorite ? "Seat added to favorites" : "Seat removed from favorites";
        return ResponseEntity.ok(new MessageResponse(message));
    }
    
    @GetMapping("/favorites")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<List<SeatDTO>> getFavoriteSeats() {
        List<SeatDTO> seats = seatService.getFavoriteSeats();
        return ResponseEntity.ok(seats);
    }
}