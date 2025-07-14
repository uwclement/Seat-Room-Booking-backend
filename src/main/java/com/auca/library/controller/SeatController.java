package com.auca.library.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.auca.library.dto.request.SeatAvailabilityRequest;
import com.auca.library.dto.response.MessageResponse;
import com.auca.library.dto.response.SeatDTO;
import com.auca.library.service.SeatService;

import jakarta.validation.Valid;

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


    @GetMapping("/Gishushu")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<List<SeatDTO>> getSeatsInGishushu() {
        List<SeatDTO> seats = seatService.getSeatsInGishushu();
        return ResponseEntity.ok(seats);
    }
    

     @GetMapping("/Masoro")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<List<SeatDTO>> getSeatsInMasoro() {
        List<SeatDTO> seats = seatService.getSeatsInMasoro();
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