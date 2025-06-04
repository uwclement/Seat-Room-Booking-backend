package com.auca.library.controller;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.auca.library.dto.request.BookingApprovalRequest;
import com.auca.library.dto.request.BulkBookingApprovalRequest;
import com.auca.library.dto.response.AdminBookingResponse;
import com.auca.library.dto.response.BulkOperationResponse;
import com.auca.library.dto.response.MessageResponse;
import com.auca.library.dto.response.QuickStatsResponse;
import com.auca.library.service.AdminRoomBookingService;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin/bookings")
@CrossOrigin(origins = "*", maxAge = 3600)
@PreAuthorize("hasRole('ADMIN')")
public class AdminRoomBookingController {

    @Autowired
    private AdminRoomBookingService adminBookingService;

    // ========== SIMPLE DATA ENDPOINTS ==========

    @GetMapping
    @Operation(summary = "Get all bookings", description = "Get all bookings for frontend filtering")
    public ResponseEntity<List<AdminBookingResponse>> getAllBookings() {
        List<AdminBookingResponse> bookings = adminBookingService.getAllBookings();
        return ResponseEntity.ok(bookings);
    }

    @GetMapping("/date-range")
    @Operation(summary = "Get bookings by date range", description = "Get bookings within a specific date range")
    public ResponseEntity<List<AdminBookingResponse>> getBookingsByDateRange(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        
        List<AdminBookingResponse> bookings = adminBookingService.getBookingsByDateRange(startDate, endDate);
        return ResponseEntity.ok(bookings);
    }

    @GetMapping("/pending")
    @Operation(summary = "Get pending bookings", description = "Get all bookings awaiting approval")
    public ResponseEntity<List<AdminBookingResponse>> getPendingBookings() {
        List<AdminBookingResponse> bookings = adminBookingService.getPendingBookings();
        return ResponseEntity.ok(bookings);
    }

    // @GetMapping("/quick-stats")
    // @Operation(summary = "Get dashboard stats", description = "Get quick statistics for admin dashboard")
    // public ResponseEntity<QuickStatsResponse> getQuickStats() {
    //     QuickStatsResponse stats = adminBookingService.getQuickStats();
    //     return ResponseEntity.ok(stats);
    // }

    // ========== APPROVAL ENDPOINTS ==========

    @PostMapping("/approve")
    @Operation(summary = "Approve or reject booking", description = "Handle booking approval or rejection")
    public ResponseEntity<MessageResponse> handleApproval(
            @Valid @RequestBody BookingApprovalRequest request,
            Authentication authentication) {
        
        String adminEmail = authentication.getName();
        MessageResponse response = adminBookingService.handleBookingApproval(request, adminEmail);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/bulk-approve")
    @Operation(summary = "Bulk approve/reject bookings", description = "Handle multiple booking approvals/rejections")
    public ResponseEntity<BulkOperationResponse> handleBulkApproval(
            @Valid @RequestBody BulkBookingApprovalRequest request,
            Authentication authentication) {
        
        String adminEmail = authentication.getName();
        BulkOperationResponse response = adminBookingService.handleBulkApproval(request, adminEmail);
        return ResponseEntity.ok(response);
    }
}