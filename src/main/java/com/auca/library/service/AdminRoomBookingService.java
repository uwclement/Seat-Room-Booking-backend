package com.auca.library.service;

import com.auca.library.dto.request.*;
import com.auca.library.dto.response.*;
import com.auca.library.exception.ResourceNotFoundException;
import com.auca.library.model.*;
import com.auca.library.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AdminRoomBookingService {

    @Autowired
    private RoomBookingRepository roomBookingRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private NotificationService notificationService;

    // ========== SIMPLE DATA RETRIEVAL METHODS ==========

    // Just get all bookings - let frontend filter
    public List<AdminBookingResponse> getAllBookings() {
        List<RoomBooking> bookings = roomBookingRepository.findAll();
        return bookings.stream()
                .map(this::mapToAdminResponse)
                .collect(Collectors.toList());
    }

    // Get bookings by date range (most common filter)
    public List<AdminBookingResponse> getBookingsByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        List<RoomBooking> bookings;
        if (startDate != null && endDate != null) {
            bookings = roomBookingRepository.findByStartTimeBetween(startDate, endDate);
        } else {
            // Default to last 30 days if no dates provided
            LocalDateTime defaultStart = LocalDateTime.now().minusDays(30);
            LocalDateTime defaultEnd = LocalDateTime.now().plusDays(7);
            bookings = roomBookingRepository.findByStartTimeBetween(defaultStart, defaultEnd);
        }
        
        return bookings.stream()
                .map(this::mapToAdminResponse)
                .collect(Collectors.toList());
    }

    // Get only pending bookings (most important for admins)
    public List<AdminBookingResponse> getPendingBookings() {
        List<RoomBooking> pendingBookings = roomBookingRepository.findPendingApprovalBookings();
        return pendingBookings.stream()
                .map(this::mapToAdminResponse)
                .collect(Collectors.toList());
    }

    // // Get basic stats for dashboard
    // public QuickStatsResponse getQuickStats() {
    //     LocalDateTime now = LocalDateTime.now();
    //     LocalDateTime today = now.toLocalDate().atStartOfDay();
    //     LocalDateTime tomorrow = today.plusDays(1);
        
    //     QuickStatsResponse stats = new QuickStatsResponse();
        
    //     // Simple counts using existing repository methods
    //     stats.setPendingApprovals(roomBookingRepository.countPendingApprovals());
    //     stats.setTodayBookings((int) roomBookingRepository.findByStartTimeBetween(today, tomorrow).size());
    //     stats.setCurrentOccupancy(roomBookingRepository.findCurrentlyActiveBooking().size());
        
    //     return stats;
    // }

    // ========== APPROVAL METHODS (using your existing DTOs) ==========

    @Transactional
    public MessageResponse handleBookingApproval(BookingApprovalRequest request, String adminEmail) {
        RoomBooking booking = findBookingById(request.getBookingId());
        User admin = findUserByEmail(adminEmail);
        
        if (booking.getStatus() != RoomBooking.BookingStatus.PENDING) {
            throw new IllegalStateException("Only pending bookings can be approved/rejected");
        }
        
        if (request.isApproved()) {
            // Approve booking
            booking.setStatus(RoomBooking.BookingStatus.CONFIRMED);
            booking.setApprovedBy(admin);
            booking.setApprovedAt(LocalDateTime.now());
            
            roomBookingRepository.save(booking);
            
            // Send notification
            notificationService.addNotification(
                    booking.getUser().getEmail(),
                    "Booking Approved",
                    String.format("Your booking '%s' has been approved", booking.getTitle()),
                    "BOOKING_APPROVED"
            );
            
            return new MessageResponse("Booking approved successfully");
            
        } else {
            // Reject booking
            booking.setStatus(RoomBooking.BookingStatus.REJECTED);
            booking.setApprovedBy(admin);
            booking.setApprovedAt(LocalDateTime.now());
            booking.setRejectionReason(request.getRejectionReason());
            
            roomBookingRepository.save(booking);
            
            // Send notification
            notificationService.addNotification(
                    booking.getUser().getEmail(),
                    "Booking Rejected",
                    String.format("Your booking '%s' has been rejected. Reason: %s", 
                            booking.getTitle(), request.getRejectionReason()),
                    "BOOKING_REJECTED"
            );
            
            return new MessageResponse("Booking rejected successfully");
        }
    }

    @Transactional
    public BulkOperationResponse handleBulkApproval(BulkBookingApprovalRequest request, String adminEmail) {
        User admin = findUserByEmail(adminEmail);
        List<RoomBooking> bookings = roomBookingRepository.findAllById(request.getBookingIds());
        
        int successCount = 0;
        int failureCount = 0;
        List<String> errors = new ArrayList<>();
        
        for (RoomBooking booking : bookings) {
            try {
                if (booking.getStatus() != RoomBooking.BookingStatus.PENDING) {
                    errors.add("Booking " + booking.getId() + " is not pending");
                    failureCount++;
                    continue;
                }
                
                if (request.isApproved()) {
                    booking.setStatus(RoomBooking.BookingStatus.CONFIRMED);
                    notificationService.addNotification(
                            booking.getUser().getEmail(),
                            "Booking Approved",
                            String.format("Your booking '%s' has been approved", booking.getTitle()),
                            "BOOKING_APPROVED"
                    );
                } else {
                    booking.setStatus(RoomBooking.BookingStatus.REJECTED);
                    booking.setRejectionReason(request.getRejectionReason());
                    notificationService.addNotification(
                            booking.getUser().getEmail(),
                            "Booking Rejected",
                            String.format("Your booking '%s' has been rejected", booking.getTitle()),
                            "BOOKING_REJECTED"
                    );
                }
                
                booking.setApprovedBy(admin);
                booking.setApprovedAt(LocalDateTime.now());
                successCount++;
                
            } catch (Exception e) {
                errors.add("Failed to process booking " + booking.getId() + ": " + e.getMessage());
                failureCount++;
            }
        }
        
        roomBookingRepository.saveAll(bookings);
        return new BulkOperationResponse(successCount, failureCount, errors);
    }

    // ========== HELPER METHODS ==========

    private RoomBooking findBookingById(Long bookingId) {
        return roomBookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found: " + bookingId));
    }

    private User findUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + email));
    }

    private AdminBookingResponse mapToAdminResponse(RoomBooking booking) {
        AdminBookingResponse response = new AdminBookingResponse();
        response.setId(booking.getId());
        response.setTitle(booking.getTitle());
        response.setDescription(booking.getDescription());
        response.setStartTime(booking.getStartTime());
        response.setEndTime(booking.getEndTime());
        response.setStatus(booking.getStatus());
        response.setCreatedAt(booking.getCreatedAt());
        response.setUpdatedAt(booking.getUpdatedAt());
        response.setRequiresApproval(booking.isRequiresApproval());
        response.setApprovedAt(booking.getApprovedAt());
        response.setRejectionReason(booking.getRejectionReason());
        response.setCheckedInAt(booking.getCheckedInAt());
        response.setPublicBooking(booking.isPublic());
        
        // Calculate duration
        response.setDurationHours(Duration.between(booking.getStartTime(), booking.getEndTime()).toHours());
        
        // Set user info
        UserResponse userResponse = new UserResponse();
        userResponse.setId(booking.getUser().getId());
        userResponse.setFullName(booking.getUser().getFullName());
        userResponse.setEmail(booking.getUser().getEmail());
        response.setUser(userResponse);
        
        // Set room info
        RoomResponse roomResponse = new RoomResponse();
        roomResponse.setId(booking.getRoom().getId());
        roomResponse.setRoomNumber(booking.getRoom().getRoomNumber());
        roomResponse.setName(booking.getRoom().getName());
        roomResponse.setCategory(booking.getRoom().getCategory());
        roomResponse.setBuilding(booking.getRoom().getBuilding());
        roomResponse.setFloor(booking.getRoom().getFloor());
        roomResponse.setCapacity(booking.getRoom().getCapacity());
        response.setRoom(roomResponse);
        
        // Set additional helper fields
        response.setBuilding(booking.getRoom().getBuilding());
        response.setFloor(booking.getRoom().getFloor());
        response.setRoomCategory(booking.getRoom().getCategory().name());
        response.setParticipantCount(booking.getParticipants().size() + 1);
        response.setOverdue(booking.isOverdue());
        
        // Set approved by if available
        if (booking.getApprovedBy() != null) {
            UserResponse approvedByResponse = new UserResponse();
            approvedByResponse.setId(booking.getApprovedBy().getId());
            approvedByResponse.setFullName(booking.getApprovedBy().getFullName());
            approvedByResponse.setEmail(booking.getApprovedBy().getEmail());
            response.setApprovedBy(approvedByResponse);
        }
        
        return response;
    }
}