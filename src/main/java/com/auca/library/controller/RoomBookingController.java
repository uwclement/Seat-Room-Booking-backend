package com.auca.library.controller;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.auca.library.dto.request.BookingSearchRequest;
import com.auca.library.dto.request.BookingUpdateRequest;
import com.auca.library.dto.request.ExtendBookingRequest;
import com.auca.library.dto.request.InvitationResponseRequest;
import com.auca.library.dto.request.InviteParticipantsRequest;
import com.auca.library.dto.request.JoinBookingRequest;
import com.auca.library.dto.request.RoomBookingRequest;
import com.auca.library.dto.response.BookingHistoryResponse;
import com.auca.library.dto.response.BookingParticipantResponse;
import com.auca.library.dto.response.InvitationResponse;
import com.auca.library.dto.response.MessageResponse;
import com.auca.library.dto.response.RecurringBookingSeriesResponse;
import com.auca.library.dto.response.RoomAvailabilityResponse;
import com.auca.library.dto.response.RoomBookingResponse;
import com.auca.library.dto.response.RoomResponse;
import com.auca.library.dto.response.UserBookingStatsResponse;
import com.auca.library.dto.response.WeeklyRoomAvailabilityResponse;
import com.auca.library.service.AdminRoomService;
import com.auca.library.service.RoomAvailabilityService;
import com.auca.library.service.RoomBookingService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/Roombookings")
@CrossOrigin(origins = "*", maxAge = 3600)
public class RoomBookingController {

    @Autowired
    private RoomBookingService roomBookingService;
    
    @Autowired
    private RoomAvailabilityService roomAvailabilityService;

    @Autowired
    private AdminRoomService adminRoomService;

    // retrieve rooms 
    @GetMapping("/rooms")
    public ResponseEntity<List<RoomResponse>> getAllRooms() {
        List<RoomResponse> rooms = adminRoomService.getAllRooms();
        return ResponseEntity.ok(rooms);
    }

    // Get single room by ID
    @GetMapping("/rooms/{roomId}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<RoomResponse> getRoomById( @Parameter(description = "Room ID") @PathVariable Long roomId) {  
    RoomResponse response = adminRoomService.getRoomById(roomId);
    return ResponseEntity.ok(response);
}
    // ========== BOOKING CRUD OPERATIONS ==========
    @PostMapping
    @Operation(summary = "Create a new room booking", description = "Create a new room booking with optional participants and recurring settings")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Booking created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid booking request"),
        @ApiResponse(responseCode = "409", description = "Booking conflict - room not available"),
        @ApiResponse(responseCode = "403", description = "User not authorized or booking limits exceeded")
    })
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN') or hasRole('LIBRARIAN')")
    public ResponseEntity<RoomBookingResponse> createBooking(
            @Valid @RequestBody RoomBookingRequest request,
            Authentication authentication) {
        
        String userEmail = authentication.getName();
        RoomBookingResponse response = roomBookingService.createBooking(request, userEmail);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PutMapping("/{bookingId}")
    @Operation(summary = "Update an existing booking", description = "Update booking details. Only the booking owner or admin can update.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Booking updated successfully"),
        @ApiResponse(responseCode = "404", description = "Booking not found"),
        @ApiResponse(responseCode = "403", description = "Not authorized to update this booking"),
        @ApiResponse(responseCode = "409", description = "Updated time conflicts with existing bookings")
    })
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN') or hasRole('LIBRARIAN')")
    public ResponseEntity<RoomBookingResponse> updateBooking(
            @Parameter(description = "Booking ID") @PathVariable Long bookingId,
            @Valid @RequestBody BookingUpdateRequest request,
            Authentication authentication) {
        
        String userEmail = authentication.getName();
        RoomBookingResponse response = roomBookingService.updateBooking(bookingId, request, userEmail);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{bookingId}")
    @Operation(summary = "Cancel a booking", description = "Cancel a booking. Only the booking owner or admin can cancel.")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<MessageResponse> cancelBooking(
            @Parameter(description = "Booking ID") @PathVariable Long bookingId,
            Authentication authentication) {
        
        String userEmail = authentication.getName();
        MessageResponse response = roomBookingService.cancelBooking(bookingId, userEmail);
        return ResponseEntity.ok(response);
    }


@GetMapping("/my-invitations")
@Operation(summary = "Get user's pending invitations", description = "Get all pending room booking invitations for the current user")
@PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
public ResponseEntity<List<InvitationResponse>> getMyPendingInvitations(
        Authentication authentication) {
    
    String userEmail = authentication.getName();
    List<InvitationResponse> invitations = roomBookingService.getUserPendingInvitations(userEmail);
    return ResponseEntity.ok(invitations);
}

    @GetMapping("/{bookingId}")
    @Operation(summary = "Get booking details", description = "Get detailed information about a specific booking")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<RoomBookingResponse> getBooking(
            @Parameter(description = "Booking ID") @PathVariable Long bookingId,
            Authentication authentication) {
        
        String userEmail = authentication.getName();
        RoomBookingResponse response = roomBookingService.getBookingById(bookingId, userEmail);
        return ResponseEntity.ok(response);
    }

    // ========== USER BOOKING MANAGEMENT ==========

    @GetMapping("/my-bookings")
    @Operation(summary = "Get user's active bookings", description = "Get all active bookings for the current user")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<List<RoomBookingResponse>> getMyBookings(Authentication authentication) {
        String userEmail = authentication.getName();
        List<RoomBookingResponse> bookings = roomBookingService.getUserBookings(userEmail);
        return ResponseEntity.ok(bookings);
    }

    @GetMapping("/my-history")
    @Operation(summary = "Get user's booking history", description = "Get complete booking history with statistics")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<BookingHistoryResponse> getMyBookingHistory(
            Pageable pageable,
            Authentication authentication) {
        
        String userEmail = authentication.getName();
        BookingHistoryResponse response = roomBookingService.getUserBookingHistory(userEmail, pageable);
        return ResponseEntity.ok(response);
    }

    // ========== CHECK-IN OPERATIONS ==========

    @PostMapping("/{bookingId}/check-in")
    @Operation(summary = "Check in to a booking", description = "Check in to a confirmed booking. Must be done within the check-in window.")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<MessageResponse> checkIn(
            @Parameter(description = "Booking ID") @PathVariable Long bookingId,
            Authentication authentication) {
        
        String userEmail = authentication.getName();
        MessageResponse response = roomBookingService.checkInToBooking(bookingId, userEmail);
        return ResponseEntity.ok(response);
    }

    // ========== PUBLIC/JOINABLE BOOKINGS ==========

    @GetMapping("/joinable")
    @Operation(summary = "Get joinable bookings", description = "Get all public bookings that allow joining")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<List<RoomBookingResponse>> getJoinableBookings() {
        List<RoomBookingResponse> bookings = roomBookingService.getJoinableBookings();
        return ResponseEntity.ok(bookings);
    }

    @PostMapping("/join")
    @Operation(summary = "Join a public booking", description = "Join a public booking if capacity allows")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<MessageResponse> joinBooking(
            @Valid @RequestBody JoinBookingRequest request,
            Authentication authentication) {
        
        String userEmail = authentication.getName();
        MessageResponse response = roomBookingService.joinBooking(request, userEmail);
        return ResponseEntity.ok(response);
    }

    // ========== ROOM AVAILABILITY ==========

    @GetMapping("/rooms/{roomId}/availability")
    @Operation(summary = "Get real-time room availability", description = "Get current availability and upcoming slots for a room")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN') or hasRole('LIBRARIAN')")
    public ResponseEntity<RoomAvailabilityResponse> getRoomAvailability(
            @Parameter(description = "Room ID") @PathVariable Long roomId) {
        
        RoomAvailabilityResponse response = roomAvailabilityService.getRoomAvailability(roomId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/rooms/{roomId}/weekly-availability")
    @Operation(summary = "Get weekly room availability", description = "Get weekly calendar view of room availability")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN') or hasRole('LIBRARIAN')")
    public ResponseEntity<WeeklyRoomAvailabilityResponse> getWeeklyAvailability(
            @Parameter(description = "Room ID") @PathVariable Long roomId,
            @Parameter(description = "Week start date (defaults to current week)")
            @RequestParam(required = false) 
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime weekStart,
            Authentication authentication) {
        
        if (weekStart == null) {
            weekStart = LocalDateTime.now().with(java.time.DayOfWeek.SUNDAY).withHour(0).withMinute(0).withSecond(0);
        }
        
        String userEmail = authentication.getName();
        WeeklyRoomAvailabilityResponse response = roomBookingService.getWeeklyAvailability(roomId, weekStart, userEmail);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/rooms/all-availability")
    @Operation(summary = "Get all rooms availability", description = "Get real-time availability for all rooms")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN') or hasRole('LIBRARIAN')")
    public ResponseEntity<List<RoomAvailabilityResponse>> getAllRoomsAvailability() {
        List<RoomAvailabilityResponse> responses = roomAvailabilityService.getAllRoomsAvailability();
        return ResponseEntity.ok(responses);
    }

    // ========== PARTICIPANT MANAGEMENT ==========

    @PostMapping("/{bookingId}/participants/invite")
    @Operation(summary = "Invite participants to booking", description = "Invite additional participants to an existing booking")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN') or hasRole('LIBRARIAN')")
    public ResponseEntity<MessageResponse> inviteParticipants(
            @Parameter(description = "Booking ID") @PathVariable Long bookingId,
            @Valid @RequestBody InviteParticipantsRequest request,
            Authentication authentication) {
        
        String userEmail = authentication.getName();
        MessageResponse response = roomBookingService.inviteParticipants(bookingId, request, userEmail);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{bookingId}/participants/{participantId}/respond")
    @Operation(summary = "Respond to booking invitation", description = "Accept or decline a booking invitation")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN') or hasRole('LIBRARIAN')")
    public ResponseEntity<MessageResponse> respondToInvitation(
            @Parameter(description = "Booking ID") @PathVariable Long bookingId,
            @Parameter(description = "Participant ID") @PathVariable Long participantId,
            @Valid @RequestBody InvitationResponseRequest request,
            Authentication authentication) {
        
        String userEmail = authentication.getName();
        MessageResponse response = roomBookingService.respondToInvitation(bookingId, participantId, request, userEmail);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{bookingId}/participants/{participantId}")
    @Operation(summary = "Remove participant from booking", description = "Remove a participant from booking (owner only)")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN') or hasRole('LIBRARIAN')")
    public ResponseEntity<MessageResponse> removeParticipant(
            @Parameter(description = "Booking ID") @PathVariable Long bookingId,
            @Parameter(description = "Participant ID") @PathVariable Long participantId,
            Authentication authentication) {
        
        String userEmail = authentication.getName();
        MessageResponse response = roomBookingService.removeParticipant(bookingId, participantId, userEmail);
        return ResponseEntity.ok(response);
    }

    // ========== RECURRING BOOKINGS ==========

    @GetMapping("/{bookingId}/recurring-series")
    @Operation(summary = "Get recurring series information", description = "Get information about the recurring booking series")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN') or hasRole('LIBRARIAN')")
    public ResponseEntity<RecurringBookingSeriesResponse> getRecurringSeries(
            @Parameter(description = "Booking ID") @PathVariable Long bookingId,
            Authentication authentication) {
        
        String userEmail = authentication.getName();
        RecurringBookingSeriesResponse response = roomBookingService.getRecurringSeries(bookingId, userEmail);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{bookingId}/recurring-series")
    @Operation(summary = "Cancel entire recurring series", description = "Cancel all future bookings in the recurring series")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN') or hasRole('LIBRARIAN')")
    public ResponseEntity<MessageResponse> cancelRecurringSeries(
            @Parameter(description = "Booking ID") @PathVariable Long bookingId,
            Authentication authentication) {
        
        String userEmail = authentication.getName();
        MessageResponse response = roomBookingService.cancelRecurringSeries(bookingId, userEmail);
        return ResponseEntity.ok(response);
    }

    // ========== SEARCH AND FILTER ==========

    @GetMapping("/search")
    @Operation(summary = "Search bookings", description = "Search bookings with various filters")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN') or hasRole('LIBRARIAN')")
    public ResponseEntity<List<RoomBookingResponse>> searchBookings(
            @Parameter(description = "Search keyword") @RequestParam(required = false) String keyword,
            @Parameter(description = "Room ID") @RequestParam(required = false) Long roomId,
            @Parameter(description = "Start date") @RequestParam(required = false) 
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @Parameter(description = "End date") @RequestParam(required = false) 
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @Parameter(description = "Booking status") @RequestParam(required = false) String status,
            @Parameter(description = "Only public bookings") @RequestParam(required = false, defaultValue = "false") boolean publicOnly,
            Authentication authentication) {
        
        String userEmail = authentication.getName();
        
        BookingSearchRequest searchRequest = new BookingSearchRequest();
        searchRequest.setKeyword(keyword);
        searchRequest.setRoomId(roomId);
        searchRequest.setStartDate(startDate);
        searchRequest.setEndDate(endDate);
        searchRequest.setStatus(status);
        searchRequest.setPublicOnly(publicOnly);
        
        List<RoomBookingResponse> bookings = roomBookingService.searchBookings(searchRequest, userEmail);
        return ResponseEntity.ok(bookings);
    }

    // ========== QUICK ACTIONS ==========

    @GetMapping("/quick-book/{roomId}")
    @Operation(summary = "Quick book available slot", description = "Find and book the next available slot for a room")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN') or hasRole('LIBRARIAN')")
    public ResponseEntity<RoomBookingResponse> quickBook(
            @Parameter(description = "Room ID") @PathVariable Long roomId,
            @Parameter(description = "Duration in hours") @RequestParam(defaultValue = "1") int durationHours,
            Authentication authentication) {
        
        String userEmail = authentication.getName();
        RoomBookingResponse response = roomBookingService.quickBook(roomId, durationHours, userEmail);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{bookingId}/extend")
    @Operation(summary = "Extend booking duration", description = "Extend an active booking if room is available")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN') or hasRole('LIBRARIAN')")
    public ResponseEntity<RoomBookingResponse> extendBooking(
            @Parameter(description = "Booking ID") @PathVariable Long bookingId,
            @Valid @RequestBody ExtendBookingRequest request,
            Authentication authentication) {
        
        String userEmail = authentication.getName();
        RoomBookingResponse response = roomBookingService.extendBooking(bookingId, request, userEmail);
        return ResponseEntity.ok(response);
    }

    // ========== STATISTICS AND ANALYTICS ==========

    @GetMapping("/my-stats")
    @Operation(summary = "Get user booking statistics", description = "Get personal booking statistics and usage patterns")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN') or hasRole('LIBRARIAN')")
    public ResponseEntity<UserBookingStatsResponse> getMyStats(
            @Parameter(description = "Number of weeks to analyze") @RequestParam(defaultValue = "4") int weeks,
            Authentication authentication) {
        
        String userEmail = authentication.getName();
        UserBookingStatsResponse response = roomBookingService.getUserStats(userEmail, weeks);
        return ResponseEntity.ok(response);
    }

    // // ========== ERROR HANDLING ==========

    // @ExceptionHandler(Exception.class)
    // public ResponseEntity<ErrorResponse> handleException(Exception e) {
    //     ErrorResponse error = new ErrorResponse();
    //     error.setMessage("An error occurred: " + e.getMessage());
    //     error.setTimestamp(LocalDateTime.now());
    //     return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    // }
}