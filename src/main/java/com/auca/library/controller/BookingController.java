package com.auca.library.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.auca.library.dto.request.CreateBookingRequest;
import com.auca.library.dto.request.ExtendBookingRequest;
import com.auca.library.dto.request.ExtensionRequest;
import com.auca.library.dto.response.BookingDTO;
import com.auca.library.dto.response.MessageResponse;
import com.auca.library.service.BookingService;

import jakarta.mail.MessagingException;
import jakarta.validation.Valid;

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
        return ResponseEntity.ok(new MessageResponse("Booking cancelled successfully" + booking ));
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


    @PostMapping("/{id}/extend")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<BookingDTO> extendBooking(
        @PathVariable Long id, 
        @Valid @RequestBody ExtendBookingRequest request) {
    BookingDTO booking = bookingService.extendBooking(id, request.getAdditionalHours());
    return ResponseEntity.ok(booking);
}

    @PutMapping("/{id}")
    public ResponseEntity<BookingDTO> updateBooking(
        @PathVariable Long id,
        @Valid @RequestBody CreateBookingRequest request) {
        try {
            BookingDTO updatedBooking = bookingService.updateBooking(id, request);
            return ResponseEntity.ok(updatedBooking);
        } catch (MessagingException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(null);
        }
    }
}
