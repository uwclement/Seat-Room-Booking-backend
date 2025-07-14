package com.auca.library.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.auca.library.dto.request.AdminCancellationRequest;
import com.auca.library.dto.request.BulkCancellationRequest;
import com.auca.library.dto.response.BookingResponse;
import com.auca.library.dto.response.MessageResponse;
import com.auca.library.service.AdminBookingService;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/admin/bookings")
@PreAuthorize("hasRole('ADMIN') or hasRole('LIBRARIAN')")
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


 // Manual check-in by admin
 
    @PostMapping("/{bookingId}/checkin")
    @Operation(summary = "Manual check-in by admin", description = "Allow admin to manually check in a user for their booking")
    public ResponseEntity<BookingResponse> manualCheckIn(
          @PathVariable Long bookingId,
          Authentication authentication) {
          String adminEmail = authentication.getName();
          BookingResponse response = adminBookingService.manualCheckIn(bookingId, adminEmail);
        return ResponseEntity.ok(response);
    }



 // Manual cancellation by admin with optional reason

   @DeleteMapping("/{bookingId}/cancel")
   @Operation(summary = "Manual cancellation by admin", description = "Allow admin to cancel any booking with optional reason")
    public ResponseEntity<BookingResponse> manualCancelBooking(
          @PathVariable Long bookingId,
          @RequestBody(required = false) AdminCancellationRequest request,
          Authentication authentication) {
    
          String adminEmail = authentication.getName();
          String reason = (request != null) ? request.getReason() : null;
    
         BookingResponse response = adminBookingService.manualCancelBooking(bookingId, reason, adminEmail);
        return ResponseEntity.ok(response);
    }


    
 // Bulk cancellation by admin
 
    @PostMapping("/bulk-cancel")
    @Operation(summary = "Bulk cancel bookings", description = "Cancel multiple bookings at once with optional reason")
    public ResponseEntity<AdminBookingService.BulkCancellationResponse> bulkCancelBookings(
        @Valid @RequestBody BulkCancellationRequest request,
        Authentication authentication) {
    
      String adminEmail = authentication.getName();
      AdminBookingService.BulkCancellationResponse response = adminBookingService.bulkCancelBookings(
        request.getBookingIds(), 
        request.getReason(), 
        adminEmail
    );
    return ResponseEntity.ok(response);
    }



     // Get bookings eligible for manual check-in

    @GetMapping("/eligible-checkin")
    @Operation(summary = "Get check-in eligible bookings", description = "Get bookings that can be manually checked in")
    public ResponseEntity<List<BookingResponse>> getBookingsEligibleForCheckIn() {
        List<BookingResponse> bookings = adminBookingService.getBookingsEligibleForCheckIn();
        return ResponseEntity.ok(bookings);
   }


 // Get bookings eligible for cancellation

    @GetMapping("/eligible-cancellation")
    @Operation(summary = "Get cancellation eligible bookings", description = "Get bookings that can be cancelled")
    public ResponseEntity<List<BookingResponse>> getBookingsEligibleForCancellation() {
        List<BookingResponse> bookings = adminBookingService.getBookingsEligibleForCancellation();
        return ResponseEntity.ok(bookings);
    }


}