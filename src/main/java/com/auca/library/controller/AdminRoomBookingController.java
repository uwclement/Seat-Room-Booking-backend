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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.auca.library.dto.request.*;
import com.auca.library.dto.response.*;
import com.auca.library.service.AdminRoomBookingService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin/Roombookings")
@CrossOrigin(origins = "*", maxAge = 3600)
@PreAuthorize("hasRole('ADMIN') or hasRole('LIBRARIAN')")
@Tag(name = "Admin Room Booking Management", description = "Enhanced admin operations for room bookings")
public class AdminRoomBookingController {

    @Autowired
    private AdminRoomBookingService adminBookingService;

    // ========== BOOKING DATA ENDPOINTS ==========

    @GetMapping
    @Operation(summary = "Get all bookings with enhanced details", 
               description = "Get all bookings with participant counts, equipment requests, and capacity warnings")
    public ResponseEntity<List<EnhancedAdminBookingResponse>> getAllBookings() {
        List<EnhancedAdminBookingResponse> bookings = adminBookingService.getAllBookings();
        return ResponseEntity.ok(bookings);
    }

    @GetMapping("/date-range")
    @Operation(summary = "Get bookings by date range with enhanced details", 
               description = "Get bookings within a specific date range with full admin visibility")
    public ResponseEntity<List<EnhancedAdminBookingResponse>> getBookingsByDateRange(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        
        List<EnhancedAdminBookingResponse> bookings = adminBookingService.getBookingsByDateRange(startDate, endDate);
        return ResponseEntity.ok(bookings);
    }

    @GetMapping("/pending")
    @Operation(summary = "Get pending bookings with capacity analysis", 
               description = "Get all bookings awaiting approval with participant and capacity details")
    public ResponseEntity<List<EnhancedAdminBookingResponse>> getPendingBookings() {
        List<EnhancedAdminBookingResponse> bookings = adminBookingService.getPendingBookings();
        return ResponseEntity.ok(bookings);
    }

    @GetMapping("/{bookingId}")
    @Operation(summary = "Get single booking with full admin details", 
               description = "Get detailed view of a specific booking including all admin-relevant information")
    public ResponseEntity<EnhancedAdminBookingResponse> getBookingDetails(@PathVariable Long bookingId) {
        // This method should be added to the service
        EnhancedAdminBookingResponse booking = adminBookingService.getBookingDetails(bookingId);
        return ResponseEntity.ok(booking);
    }

    // ========== BOOKING APPROVAL ENDPOINTS ==========

    @PostMapping("/approve")
    @Operation(summary = "Approve or reject booking with capacity consideration", 
               description = "Handle booking approval or rejection with capacity validation and override capability")
    public ResponseEntity<MessageResponse> handleApproval(
            @Valid @RequestBody BookingApprovalRequest request,
            Authentication authentication) {
        
        String adminEmail = authentication.getName();
        MessageResponse response = adminBookingService.handleBookingApproval(request, adminEmail);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/bulk-approve")
    @Operation(summary = "Bulk approve/reject bookings", 
               description = "Handle multiple booking approvals/rejections with capacity warnings")
    public ResponseEntity<BulkOperationResponse> handleBulkApproval(
            @Valid @RequestBody BulkBookingApprovalRequest request,
            Authentication authentication) {
        
        String adminEmail = authentication.getName();
        BulkOperationResponse response = adminBookingService.handleBulkApproval(request, adminEmail);
        return ResponseEntity.ok(response);
    }

    // ========== EQUIPMENT APPROVAL ENDPOINTS ==========

    @PostMapping("/equipment/approve")
    @Operation(summary = "Approve or reject equipment request", 
               description = "Handle individual equipment request approval or rejection for a booking")
    public ResponseEntity<MessageResponse> handleEquipmentApproval(
            @Valid @RequestBody EquipmentApprovalRequest request,
            Authentication authentication) {
        
        String adminEmail = authentication.getName();
        MessageResponse response = adminBookingService.handleEquipmentApproval(request, adminEmail);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/equipment/bulk-approve")
    @Operation(summary = "Bulk approve/reject equipment requests", 
               description = "Handle multiple equipment request approvals/rejections across multiple bookings")
    public ResponseEntity<EquipmentOperationResponse> handleBulkEquipmentApproval(
            @Valid @RequestBody BulkEquipmentApprovalRequest request,
            Authentication authentication) {
        
        String adminEmail = authentication.getName();
        EquipmentOperationResponse response = adminBookingService.handleBulkEquipmentApproval(request, adminEmail);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{bookingId}/equipment")
    @Operation(summary = "Get equipment requests for a booking", 
               description = "Get all equipment requests and their approval status for a specific booking")
    public ResponseEntity<List<EquipmentApprovalResponse>> getBookingEquipmentRequests(@PathVariable Long bookingId) {
        List<EquipmentApprovalResponse> equipmentRequests = adminBookingService.getBookingEquipmentRequests(bookingId);
        return ResponseEntity.ok(equipmentRequests);
    }

    // ========== PARTICIPANT ANALYSIS ENDPOINTS ==========

    @GetMapping("/{bookingId}/participants")
    @Operation(summary = "Get participant summary for a booking", 
               description = "Get detailed participant analysis including capacity warnings")
    public ResponseEntity<ParticipantSummaryResponse> getBookingParticipantSummary(@PathVariable Long bookingId) {
        ParticipantSummaryResponse summary = adminBookingService.getBookingParticipantSummary(bookingId);
        return ResponseEntity.ok(summary);
    }

    @GetMapping("/capacity-warnings")
    @Operation(summary = "Get bookings with capacity warnings", 
               description = "Get all bookings that have capacity issues (under or over capacity)")
    public ResponseEntity<List<EnhancedAdminBookingResponse>> getBookingsWithCapacityWarnings() {
        List<EnhancedAdminBookingResponse> bookings = adminBookingService.getBookingsWithCapacityWarnings();
        return ResponseEntity.ok(bookings);
    }

    // ========== ADMIN CANCELLATION ENDPOINTS ==========

    @PostMapping("/cancel")
    @Operation(summary = "Cancel approved booking as admin", 
               description = "Cancel an approved booking with reason and notification options")
    public ResponseEntity<MessageResponse> cancelBookingAsAdmin(
            @Valid @RequestBody AdminBookingCancellationRequest request,
            Authentication authentication) {
        
        String adminEmail = authentication.getName();
        MessageResponse response = adminBookingService.cancelBookingAsAdmin(request, adminEmail);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/bulk-cancel")
    @Operation(summary = "Bulk cancel approved bookings as admin", 
               description = "Cancel multiple approved bookings with reason and notification options")
    public ResponseEntity<BulkOperationResponse> cancelBookingsAsAdmin(
            @Valid @RequestBody BulkAdminCancellationRequest request,
            Authentication authentication) {
        
        String adminEmail = authentication.getName();
        BulkOperationResponse response = adminBookingService.cancelBookingsAsAdmin(request, adminEmail);
        return ResponseEntity.ok(response);
    }

    // ========== ANALYTICS AND REPORTING ENDPOINTS ==========

    @GetMapping("/analytics/equipment-usage")
    @Operation(summary = "Get equipment usage analytics", 
               description = "Get statistics on equipment request and approval rates")
    public ResponseEntity<EquipmentUsageAnalyticsResponse> getEquipmentUsageAnalytics(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        
        EquipmentUsageAnalyticsResponse analytics = adminBookingService.getEquipmentUsageAnalytics(startDate, endDate);
        return ResponseEntity.ok(analytics);
    }

    @GetMapping("/analytics/capacity-utilization")
    @Operation(summary = "Get room capacity utilization analytics", 
               description = "Get statistics on how well room capacities are being utilized")
    public ResponseEntity<CapacityUtilizationAnalyticsResponse> getCapacityUtilizationAnalytics(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        
        CapacityUtilizationAnalyticsResponse analytics = adminBookingService.getCapacityUtilizationAnalytics(startDate, endDate);
        return ResponseEntity.ok(analytics);
    }

    @GetMapping("/analytics/approval-stats")
    @Operation(summary = "Get booking approval statistics", 
               description = "Get statistics on booking approval rates, times, and reasons")
    public ResponseEntity<ApprovalStatisticsResponse> getApprovalStatistics(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        
        ApprovalStatisticsResponse stats = adminBookingService.getApprovalStatistics(startDate, endDate);
        return ResponseEntity.ok(stats);
    }

    // ========== QUICK ACTION ENDPOINTS ==========

    @PostMapping("/quick-actions/approve-all-pending")
    @Operation(summary = "Quick approve all pending bookings", 
               description = "Approve all currently pending bookings with capacity warnings")
    public ResponseEntity<BulkOperationResponse> approveAllPendingBookings(
            @RequestParam(required = false) String reason,
            Authentication authentication) {
        
        String adminEmail = authentication.getName();
        BulkOperationResponse response = adminBookingService.approveAllPendingBookings(reason, adminEmail);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/quick-actions/approve-within-capacity")
    @Operation(summary = "Approve only bookings meeting capacity requirements", 
               description = "Approve pending bookings that meet room capacity requirements")
    public ResponseEntity<BulkOperationResponse> approveBookingsWithinCapacity(
            Authentication authentication) {
        
        String adminEmail = authentication.getName();
        BulkOperationResponse response = adminBookingService.approveBookingsWithinCapacity(adminEmail);
        return ResponseEntity.ok(response);
    }

    // ========== NOTIFICATION ENDPOINTS ==========

    @PostMapping("/notifications/send-reminder")
    @Operation(summary = "Send custom reminder to booking participants", 
               description = "Send a custom reminder notification to booking organizer and participants")
    public ResponseEntity<MessageResponse> sendCustomReminder(
            @RequestParam Long bookingId,
            @RequestParam String message,
            @RequestParam(defaultValue = "true") boolean includeParticipants,
            Authentication authentication) {
        
        String adminEmail = authentication.getName();
        MessageResponse response = adminBookingService.sendCustomReminder(bookingId, message, includeParticipants, adminEmail);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/notifications/broadcast")
    @Operation(summary = "Broadcast message to all booking users", 
               description = "Send a broadcast message to all users with active bookings")
    public ResponseEntity<MessageResponse> broadcastToActiveBookingUsers(
            @RequestParam String title,
            @RequestParam String message,
            Authentication authentication) {
        
        String adminEmail = authentication.getName();
        MessageResponse response = adminBookingService.broadcastToActiveBookingUsers(title, message, adminEmail);
        return ResponseEntity.ok(response);
    }
}