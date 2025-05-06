package com.auca.library.controller;

import com.auca.library.dto.request.CreateBookingRequest;
import com.auca.library.dto.request.ExtensionRequest;
import com.auca.library.dto.response.BookingDTO;
import com.auca.library.dto.response.MessageResponse;
import com.auca.library.service.BookingService;
import jakarta.mail.MessagingException;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    @Autowired
    private BookingService bookingService;
    
    @PostMapping
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<BookingDTO> createBooking(@Valid @RequestBody CreateBookingRequest request) throws MessagingException {
        BookingDTO booking = bookingService.createBooking(request);
        return ResponseEntity.ok(booking);
    }
    
    @GetMapping
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<List<BookingDTO>> getCurrentUserBookings() {
        List<BookingDTO> bookings = bookingService.getCurrentUserBookings();
        return ResponseEntity.ok(bookings);
    }
    
    @GetMapping("/past")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<List<BookingDTO>> getPastBookings() {
        List<BookingDTO> bookings = bookingService.getPastBookings();
        return ResponseEntity.ok(bookings);
    }
    
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<BookingDTO> getBookingById(@PathVariable Long id) {
        BookingDTO booking = bookingService.getBookingById(id);
        return ResponseEntity.ok(booking);
    }
    
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<MessageResponse> cancelBooking(@PathVariable Long id) {
        BookingDTO booking = bookingService.cancelBooking(id);
        return ResponseEntity.ok(new MessageResponse("Booking cancelled successfully"));
    }
    
    @PostMapping("/{id}/checkin")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<BookingDTO> checkIn(@PathVariable Long id) {
        BookingDTO booking = bookingService.checkIn(id);
        return ResponseEntity.ok(booking);
    }
    
    @PostMapping("/{id}/checkout")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<BookingDTO> checkOut(@PathVariable Long id) {
        BookingDTO booking = bookingService.checkOut(id);
        return ResponseEntity.ok(booking);
    }
    
    @PostMapping("/extension")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<BookingDTO> respondToExtension(@Valid @RequestBody ExtensionRequest request) {
        BookingDTO booking = bookingService.respondToExtension(request);
        return ResponseEntity.ok(booking);
    }
}
