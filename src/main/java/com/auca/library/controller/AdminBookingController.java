package com.auca.library.controller;

import com.auca.library.dto.response.BookingResponse;
import com.auca.library.dto.response.MessageResponse;
import com.auca.library.service.AdminBookingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/admin/bookings")
@PreAuthorize("hasRole('ADMIN')")
public class AdminBookingController {

    @Autowired
    private AdminBookingService adminBookingService;

    // Get all current bookings
    @GetMapping("/current")
    public ResponseEntity<List<BookingResponse>> getCurrentBookings() {
        return ResponseEntity.ok(adminBookingService.getCurrentBookings());
    }

    // Get all bookings for a specific date
    @GetMapping("/date/{date}")
    public ResponseEntity<List<BookingResponse>> getBookingsByDate(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(adminBookingService.getBookingsByDate(date));
    }

    // Get all bookings for a date range
    @GetMapping("/range")
    public ResponseEntity<List<BookingResponse>> getBookingsInRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        return ResponseEntity.ok(adminBookingService.getBookingsInDateRange(start, end));
    }

    // Cancel a booking (admin override)
    @DeleteMapping("/{id}")
    public ResponseEntity<MessageResponse> cancelBooking(@PathVariable Long id) {
        return ResponseEntity.ok(adminBookingService.cancelBooking(id));
    }

    // Get bookings by user
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<BookingResponse>> getBookingsByUser(@PathVariable Long userId) {
        return ResponseEntity.ok(adminBookingService.getBookingsByUser(userId));
    }

    // Get bookings for a specific seat
    @GetMapping("/seat/{seatId}")
    public ResponseEntity<List<BookingResponse>> getBookingsBySeat(@PathVariable Long seatId) {
        return ResponseEntity.ok(adminBookingService.getBookingsBySeat(seatId));
    }
}