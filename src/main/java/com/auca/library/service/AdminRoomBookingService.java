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
    private EquipmentRepository equipmentRepository;
    @Autowired
    private BookingParticipantRepository participantRepository;
    @Autowired
    private NotificationService notificationService;

    public List<EnhancedAdminBookingResponse> getAllBookings() {
        List<RoomBooking> bookings = roomBookingRepository.findAll();
        return bookings.stream()
                .map(this::mapToEnhancedAdminResponse)
                .collect(Collectors.toList());
    }

    public List<EnhancedAdminBookingResponse> getBookingsByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        List<RoomBooking> bookings;
        if (startDate != null && endDate != null) {
            bookings = roomBookingRepository.findByStartTimeBetween(startDate, endDate);
        } else {
            LocalDateTime defaultStart = LocalDateTime.now().minusDays(30);
            LocalDateTime defaultEnd = LocalDateTime.now().plusDays(7);
            bookings = roomBookingRepository.findByStartTimeBetween(defaultStart, defaultEnd);
        }
        
        return bookings.stream()
                .map(this::mapToEnhancedAdminResponse)
                .collect(Collectors.toList());
    }

    public List<EnhancedAdminBookingResponse> getPendingBookings() {
        List<RoomBooking> pendingBookings = roomBookingRepository.findPendingApprovalBookings();
        return pendingBookings.stream()
                .map(this::mapToEnhancedAdminResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public MessageResponse handleBookingApproval(BookingApprovalRequest request, String adminEmail) {
        RoomBooking booking = findBookingById(request.getBookingId());
        User admin = findUserByEmail(adminEmail);
        
        if (booking.getStatus() != RoomBooking.BookingStatus.PENDING) {
            throw new IllegalStateException("Only pending bookings can be approved/rejected");
        }
        
        // Check capacity before approval
        ParticipantSummaryResponse participantSummary = calculateParticipantSummary(booking);
        
        if (request.isApproved()) {
            booking.setStatus(RoomBooking.BookingStatus.CONFIRMED);
            booking.setApprovedBy(admin);
            booking.setApprovedAt(LocalDateTime.now());
            
            roomBookingRepository.save(booking);
            
            // Send notification with capacity info if needed
            String notificationMessage = String.format("Your booking '%s' has been approved", booking.getTitle());
            if (!participantSummary.getCapacityMet()) {
                notificationMessage += String.format(" (Note: Room capacity is %d but only %d participants confirmed)", 
                    participantSummary.getRoomCapacity(), participantSummary.getTotalAccepted() + 1);
            }
            
            notificationService.addNotification(
                    booking.getUser().getEmail(),
                    "Booking Approved",
                    notificationMessage,
                    "BOOKING_APPROVED"
            );
            
            return new MessageResponse("Booking approved successfully" + 
                (!participantSummary.getCapacityMet() ? " (with capacity warning)" : ""));
            
        } else {
            booking.setStatus(RoomBooking.BookingStatus.REJECTED);
            booking.setApprovedBy(admin);
            booking.setApprovedAt(LocalDateTime.now());
            booking.setRejectionReason(request.getRejectionReason());
            
            roomBookingRepository.save(booking);
            
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
                
                ParticipantSummaryResponse participantSummary = calculateParticipantSummary(booking);
                
                if (request.isApproved()) {
                    booking.setStatus(RoomBooking.BookingStatus.CONFIRMED);
                    String notificationMessage = String.format("Your booking '%s' has been approved", booking.getTitle());
                    if (!participantSummary.getCapacityMet()) {
                        notificationMessage += " (Note: Room capacity not fully met)";
                    }
                    
                    notificationService.addNotification(
                            booking.getUser().getEmail(),
                            "Booking Approved",
                            notificationMessage,
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

    // ========== NEW FEATURE: EQUIPMENT APPROVAL ==========

    @Transactional
    public MessageResponse handleEquipmentApproval(EquipmentApprovalRequest request, String adminEmail) {
        RoomBooking booking = findBookingById(request.getBookingId());
        Equipment equipment = findEquipmentById(request.getEquipmentId());
        User admin = findUserByEmail(adminEmail);
        
        // Verify equipment is actually requested for this booking
        if (!booking.getRequestedEquipment().contains(equipment)) {
            throw new IllegalStateException("Equipment not requested for this booking");
        }
        
        // For now, we'll use a simple approach - store approval info in booking notes
        // In a full implementation, you'd want a separate BookingEquipmentApproval entity
        
        if (request.getApproved()) {
            // Equipment approved - keep it in the requested equipment set
            notificationService.addNotification(
                    booking.getUser().getEmail(),
                    "Equipment Request Approved",
                    String.format("Your equipment request for '%s' in booking '%s' has been approved", 
                            equipment.getName(), booking.getTitle()),
                    "EQUIPMENT_APPROVED"
            );
            
            return new MessageResponse("Equipment request approved successfully");
        } else {
            // Equipment rejected - remove from requested equipment set
            booking.getRequestedEquipment().remove(equipment);
            roomBookingRepository.save(booking);
            
            String notificationMessage = String.format("Your equipment request for '%s' in booking '%s' has been rejected", 
                    equipment.getName(), booking.getTitle());
            if (request.getReason() != null && !request.getReason().trim().isEmpty()) {
                notificationMessage += ". Reason: " + request.getReason();
            }
            
            notificationService.addNotification(
                    booking.getUser().getEmail(),
                    "Equipment Request Rejected",
                    notificationMessage,
                    "EQUIPMENT_REJECTED"
            );
            
            return new MessageResponse("Equipment request rejected successfully");
        }
    }

    @Transactional
    public EquipmentOperationResponse handleBulkEquipmentApproval(BulkEquipmentApprovalRequest request, String adminEmail) {
        User admin = findUserByEmail(adminEmail);
        List<RoomBooking> bookings = roomBookingRepository.findAllById(request.getBookingIds());
        List<Equipment> equipmentList = equipmentRepository.findAllById(request.getEquipmentIds());
        
        int successCount = 0;
        int failureCount = 0;
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        
        for (RoomBooking booking : bookings) {
            for (Equipment equipment : equipmentList) {
                try {
                    if (!booking.getRequestedEquipment().contains(equipment)) {
                        warnings.add("Equipment " + equipment.getName() + " not requested for booking " + booking.getId());
                        continue;
                    }
                    
                    if (request.getApproved()) {
                        // Keep equipment in the set (approved)
                        notificationService.addNotification(
                                booking.getUser().getEmail(),
                                "Equipment Request Approved",
                                String.format("Equipment '%s' approved for booking '%s'", 
                                        equipment.getName(), booking.getTitle()),
                                "EQUIPMENT_APPROVED"
                        );
                    } else {
                        // Remove equipment from the set (rejected)
                        booking.getRequestedEquipment().remove(equipment);
                        notificationService.addNotification(
                                booking.getUser().getEmail(),
                                "Equipment Request Rejected",
                                String.format("Equipment '%s' rejected for booking '%s'", 
                                        equipment.getName(), booking.getTitle()),
                                "EQUIPMENT_REJECTED"
                        );
                    }
                    
                    successCount++;
                    
                } catch (Exception e) {
                    errors.add("Failed to process equipment " + equipment.getName() + 
                              " for booking " + booking.getId() + ": " + e.getMessage());
                    failureCount++;
                }
            }
        }
        
        roomBookingRepository.saveAll(bookings);
        
        EquipmentOperationResponse response = new EquipmentOperationResponse(successCount, failureCount, errors);
        response.setWarnings(warnings);
        return response;
    }

    // ========== NEW FEATURE: ADMIN BOOKING CANCELLATION ==========

    @Transactional
    public MessageResponse cancelBookingAsAdmin(AdminBookingCancellationRequest request, String adminEmail) {
        RoomBooking booking = findBookingById(request.getBookingId());
        User admin = findUserByEmail(adminEmail);
        
        if (booking.getStatus() == RoomBooking.BookingStatus.CANCELLED || 
            booking.getStatus() == RoomBooking.BookingStatus.COMPLETED) {
            throw new IllegalStateException("Cannot cancel booking with status: " + booking.getStatus());
        }
        
        // Store original status for notification
        RoomBooking.BookingStatus originalStatus = booking.getStatus();
        
        // Cancel the booking
        booking.setStatus(RoomBooking.BookingStatus.CANCELLED);
        // Store admin cancellation info (you might want to add these fields to RoomBooking entity)
        booking.setRejectionReason("Admin Cancelled: " + request.getCancellationReason());
        roomBookingRepository.save(booking);
        
        // Notify booking owner
        notificationService.addNotification(
                booking.getUser().getEmail(),
                "Booking Cancelled by Admin",
                String.format("Your booking '%s' has been cancelled by an administrator. Reason: %s", 
                        booking.getTitle(), request.getCancellationReason()),
                "ADMIN_BOOKING_CANCELLED"
        );
        
        // Notify participants if requested
        if (request.getNotifyParticipants()) {
            booking.getParticipants().forEach(participant -> {
                if (participant.getStatus() == BookingParticipant.ParticipantStatus.ACCEPTED) {
                    notificationService.addNotification(
                            participant.getUser().getEmail(),
                            "Booking Cancelled by Admin",
                            String.format("The booking '%s' you were participating in has been cancelled by an administrator. Reason: %s", 
                                    booking.getTitle(), request.getCancellationReason()),
                            "ADMIN_BOOKING_CANCELLED"
                    );
                }
            });
        }
        
        return new MessageResponse("Booking cancelled successfully");
    }

    @Transactional
    public BulkOperationResponse cancelBookingsAsAdmin(BulkAdminCancellationRequest request, String adminEmail) {
        User admin = findUserByEmail(adminEmail);
        List<RoomBooking> bookings = roomBookingRepository.findAllById(request.getBookingIds());
        
        int successCount = 0;
        int failureCount = 0;
        List<String> errors = new ArrayList<>();
        
        for (RoomBooking booking : bookings) {
            try {
                if (booking.getStatus() == RoomBooking.BookingStatus.CANCELLED || 
                    booking.getStatus() == RoomBooking.BookingStatus.COMPLETED) {
                    errors.add("Cannot cancel booking " + booking.getId() + " with status: " + booking.getStatus());
                    failureCount++;
                    continue;
                }
                
                booking.setStatus(RoomBooking.BookingStatus.CANCELLED);
                booking.setRejectionReason("Admin Cancelled: " + request.getCancellationReason());
                
                // Notify booking owner
                notificationService.addNotification(
                        booking.getUser().getEmail(),
                        "Booking Cancelled by Admin",
                        String.format("Your booking '%s' has been cancelled by an administrator. Reason: %s", 
                                booking.getTitle(), request.getCancellationReason()),
                        "ADMIN_BOOKING_CANCELLED"
                );
                
                // Notify participants if requested
                if (request.getNotifyParticipants()) {
                    booking.getParticipants().forEach(participant -> {
                        if (participant.getStatus() == BookingParticipant.ParticipantStatus.ACCEPTED) {
                            notificationService.addNotification(
                                    participant.getUser().getEmail(),
                                    "Booking Cancelled by Admin",
                                    String.format("The booking '%s' has been cancelled by an administrator", 
                                            booking.getTitle()),
                                    "ADMIN_BOOKING_CANCELLED"
                            );
                        }
                    });
                }
                
                successCount++;
                
            } catch (Exception e) {
                errors.add("Failed to cancel booking " + booking.getId() + ": " + e.getMessage());
                failureCount++;
            }
        }
        
        roomBookingRepository.saveAll(bookings);
        return new BulkOperationResponse(successCount, failureCount, errors);
    }

    // ========== HELPER METHODS ==========

    private ParticipantSummaryResponse calculateParticipantSummary(RoomBooking booking) {
        ParticipantSummaryResponse summary = new ParticipantSummaryResponse();
        
        List<BookingParticipant> participants = new ArrayList<>(booking.getParticipants());
        
        summary.setTotalInvited(participants.size());
        summary.setTotalAccepted((int) participants.stream()
                .filter(p -> p.getStatus() == BookingParticipant.ParticipantStatus.ACCEPTED)
                .count());
        summary.setTotalDeclined((int) participants.stream()
                .filter(p -> p.getStatus() == BookingParticipant.ParticipantStatus.DECLINED)
                .count());
        summary.setTotalPending((int) participants.stream()
                .filter(p -> p.getStatus() == BookingParticipant.ParticipantStatus.INVITED)
                .count());
        
        summary.setRoomCapacity(booking.getRoom().getCapacity());
        
        // Calculate if capacity is met (accepted participants + organizer)
        int totalConfirmed = summary.getTotalAccepted() + 1; // +1 for organizer
        summary.setCapacityMet(totalConfirmed >= summary.getRoomCapacity());
        
        if (!summary.getCapacityMet()) {
            summary.setCapacityWarning(String.format(
                "Room capacity (%d) not met. Only %d participants confirmed (including organizer)", 
                summary.getRoomCapacity(), totalConfirmed));
        }
        
        return summary;
    }

    private EnhancedAdminBookingResponse mapToEnhancedAdminResponse(RoomBooking booking) {
        EnhancedAdminBookingResponse response = new EnhancedAdminBookingResponse();
        
        // Copy basic fields from AdminBookingResponse mapping
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
        
        // Set basic helper fields
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
        
        // NEW: Equipment approval info
        response.setHasEquipmentRequests(!booking.getRequestedEquipment().isEmpty());
        response.setPendingEquipmentCount(booking.getRequestedEquipment().size());
        
        List<EquipmentApprovalResponse> equipmentApprovals = booking.getRequestedEquipment().stream()
                .map(equipment -> {
                    EquipmentApprovalResponse equipApproval = new EquipmentApprovalResponse();
                    equipApproval.setEquipmentId(equipment.getId());
                    equipApproval.setEquipmentName(equipment.getName());
                    // For now, all requested equipment is considered "pending approval"
                    // In a full implementation, you'd track individual approval status
                    equipApproval.setApproved(null); // null = pending
                    return equipApproval;
                })
                .collect(Collectors.toList());
        response.setEquipmentApprovals(equipmentApprovals);
        
        // NEW: Participant summary
        ParticipantSummaryResponse participantSummary = calculateParticipantSummary(booking);
        response.setParticipantSummary(participantSummary);
        
        // NEW: Capacity warnings
        response.setHasCapacityWarning(!participantSummary.getCapacityMet());
        response.setCapacityWarningMessage(participantSummary.getCapacityWarning());
        
        // NEW: Admin action permissions
        response.setCanApproveEquipment(response.getHasEquipmentRequests() && 
                (booking.getStatus() == RoomBooking.BookingStatus.PENDING || 
                 booking.getStatus() == RoomBooking.BookingStatus.CONFIRMED));
        response.setCanCancelBooking(booking.getStatus() != RoomBooking.BookingStatus.CANCELLED && 
                booking.getStatus() != RoomBooking.BookingStatus.COMPLETED);
        
        return response;
    }

    private RoomBooking findBookingById(Long bookingId) {
        return roomBookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found: " + bookingId));
    }

    private User findUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + email));
    }

    private Equipment findEquipmentById(Long equipmentId) {
        return equipmentRepository.findById(equipmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Equipment not found: " + equipmentId));
    }

    // ========== ADDITIONAL SERVICE METHODS FOR CONTROLLER ==========

    public EnhancedAdminBookingResponse getBookingDetails(Long bookingId) {
        RoomBooking booking = findBookingById(bookingId);
        return mapToEnhancedAdminResponse(booking);
    }

    public List<EquipmentApprovalResponse> getBookingEquipmentRequests(Long bookingId) {
        RoomBooking booking = findBookingById(bookingId);
        return booking.getRequestedEquipment().stream()
                .map(equipment -> {
                    EquipmentApprovalResponse equipApproval = new EquipmentApprovalResponse();
                    equipApproval.setEquipmentId(equipment.getId());
                    equipApproval.setEquipmentName(equipment.getName());
                    equipApproval.setApproved(null); // null = pending approval
                    return equipApproval;
                })
                .collect(Collectors.toList());
    }

    public ParticipantSummaryResponse getBookingParticipantSummary(Long bookingId) {
        RoomBooking booking = findBookingById(bookingId);
        return calculateParticipantSummary(booking);
    }

    public List<EnhancedAdminBookingResponse> getBookingsWithCapacityWarnings() {
        List<RoomBooking> allBookings = roomBookingRepository.findAll();
        return allBookings.stream()
                .filter(booking -> {
                    ParticipantSummaryResponse summary = calculateParticipantSummary(booking);
                    return !summary.getCapacityMet();
                })
                .map(this::mapToEnhancedAdminResponse)
                .collect(Collectors.toList());
    }

    // ========== ANALYTICS METHODS ==========

    public EquipmentUsageAnalyticsResponse getEquipmentUsageAnalytics(LocalDateTime startDate, LocalDateTime endDate) {
        if (startDate == null) startDate = LocalDateTime.now().minusDays(30);
        if (endDate == null) endDate = LocalDateTime.now();

        List<RoomBooking> bookings = roomBookingRepository.findByStartTimeBetween(startDate, endDate);
        
        EquipmentUsageAnalyticsResponse analytics = new EquipmentUsageAnalyticsResponse();
        
        // Calculate equipment request statistics
        Map<String, Long> equipmentRequestCounts = bookings.stream()
                .flatMap(booking -> booking.getRequestedEquipment().stream())
                .collect(Collectors.groupingBy(Equipment::getName, Collectors.counting()));
        
        analytics.setTotalEquipmentRequests(equipmentRequestCounts.values().stream()
                .mapToLong(Long::longValue).sum());
        analytics.setUniqueEquipmentTypes(equipmentRequestCounts.size());
        analytics.setMostRequestedEquipment(equipmentRequestCounts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("None"));
        
        return analytics;
    }

    public CapacityUtilizationAnalyticsResponse getCapacityUtilizationAnalytics(LocalDateTime startDate, LocalDateTime endDate) {
        if (startDate == null) startDate = LocalDateTime.now().minusDays(30);
        if (endDate == null) endDate = LocalDateTime.now();

        List<RoomBooking> bookings = roomBookingRepository.findByStartTimeBetween(startDate, endDate);
        
        CapacityUtilizationAnalyticsResponse analytics = new CapacityUtilizationAnalyticsResponse();
        
        long totalBookings = bookings.size();
        long underCapacityBookings = bookings.stream()
                .mapToLong(booking -> {
                    ParticipantSummaryResponse summary = calculateParticipantSummary(booking);
                    return summary.getCapacityMet() ? 0 : 1;
                })
                .sum();
        
        analytics.setTotalBookings(totalBookings);
        analytics.setUnderCapacityBookings(underCapacityBookings);
        analytics.setCapacityUtilizationRate(totalBookings > 0 ? 
                (double)(totalBookings - underCapacityBookings) / totalBookings * 100 : 0);
        
        return analytics;
    }

    public ApprovalStatisticsResponse getApprovalStatistics(LocalDateTime startDate, LocalDateTime endDate) {
        if (startDate == null) startDate = LocalDateTime.now().minusDays(30);
        if (endDate == null) endDate = LocalDateTime.now();

        List<RoomBooking> bookings = roomBookingRepository.findByStartTimeBetween(startDate, endDate);
        
        ApprovalStatisticsResponse stats = new ApprovalStatisticsResponse();
        
        long totalRequiringApproval = bookings.stream()
                .filter(RoomBooking::isRequiresApproval)
                .count();
        
        long approvedCount = bookings.stream()
                .filter(booking -> booking.getStatus() == RoomBooking.BookingStatus.CONFIRMED)
                .count();
        
        long rejectedCount = bookings.stream()
                .filter(booking -> booking.getStatus() == RoomBooking.BookingStatus.REJECTED)
                .count();
        
        long pendingCount = bookings.stream()
                .filter(booking -> booking.getStatus() == RoomBooking.BookingStatus.PENDING)
                .count();
        
        stats.setTotalRequiringApproval(totalRequiringApproval);
        stats.setApprovedCount(approvedCount);
        stats.setRejectedCount(rejectedCount);
        stats.setPendingCount(pendingCount);
        stats.setApprovalRate(totalRequiringApproval > 0 ? 
                (double)approvedCount / totalRequiringApproval * 100 : 0);
        
        return stats;
    }

    // ========== QUICK ACTION METHODS ==========

    @Transactional
    public BulkOperationResponse approveAllPendingBookings(String reason, String adminEmail) {
        User admin = findUserByEmail(adminEmail);
        List<RoomBooking> pendingBookings = roomBookingRepository.findPendingApprovalBookings();
        
        BulkBookingApprovalRequest bulkRequest = new BulkBookingApprovalRequest();
        bulkRequest.setBookingIds(pendingBookings.stream().map(RoomBooking::getId).collect(Collectors.toList()));
        bulkRequest.setApproved(true);
        bulkRequest.setRejectionReason(reason);
        
        return handleBulkApproval(bulkRequest, adminEmail);
    }

    @Transactional
    public BulkOperationResponse approveBookingsWithinCapacity(String adminEmail) {
        List<RoomBooking> pendingBookings = roomBookingRepository.findPendingApprovalBookings();
        
        List<Long> bookingsToApprove = pendingBookings.stream()
                .filter(booking -> {
                    ParticipantSummaryResponse summary = calculateParticipantSummary(booking);
                    return summary.getCapacityMet();
                })
                .map(RoomBooking::getId)
                .collect(Collectors.toList());
        
        if (bookingsToApprove.isEmpty()) {
            return new BulkOperationResponse(0, 0, Arrays.asList("No bookings meet capacity requirements"));
        }
        
        BulkBookingApprovalRequest bulkRequest = new BulkBookingApprovalRequest();
        bulkRequest.setBookingIds(bookingsToApprove);
        bulkRequest.setApproved(true);
        bulkRequest.setRejectionReason("Auto-approved: Capacity requirements met");
        
        return handleBulkApproval(bulkRequest, adminEmail);
    }

    // ========== NOTIFICATION METHODS ==========

    public MessageResponse sendCustomReminder(Long bookingId, String message, boolean includeParticipants, String adminEmail) {
        RoomBooking booking = findBookingById(bookingId);
        
        // Send to booking organizer
        notificationService.addNotification(
                booking.getUser().getEmail(),
                "Admin Reminder",
                String.format("Reminder for your booking '%s': %s", booking.getTitle(), message),
                "ADMIN_REMINDER"
        );
        
        int notificationsSent = 1;
        
        // Send to participants if requested
        if (includeParticipants) {
            booking.getParticipants().forEach(participant -> {
                if (participant.getStatus() == BookingParticipant.ParticipantStatus.ACCEPTED) {
                    notificationService.addNotification(
                            participant.getUser().getEmail(),
                            "Admin Reminder",
                            String.format("Reminder for booking '%s': %s", booking.getTitle(), message),
                            "ADMIN_REMINDER"
                    );
                }
            });
            notificationsSent += (int) booking.getParticipants().stream()
                    .filter(p -> p.getStatus() == BookingParticipant.ParticipantStatus.ACCEPTED)
                    .count();
        }
        
        return new MessageResponse(String.format("Reminder sent to %d recipients", notificationsSent));
    }

    public MessageResponse broadcastToActiveBookingUsers(String title, String message, String adminEmail) {
        LocalDateTime now = LocalDateTime.now();
        List<RoomBooking> activeBookings = roomBookingRepository.findCurrentlyActiveBookings(now);
        
        Set<String> userEmails = activeBookings.stream()
                .map(booking -> booking.getUser().getEmail())
                .collect(Collectors.toSet());
        
        // Also include participants in active bookings
        activeBookings.forEach(booking -> {
            booking.getParticipants().forEach(participant -> {
                if (participant.getStatus() == BookingParticipant.ParticipantStatus.ACCEPTED) {
                    userEmails.add(participant.getUser().getEmail());
                }
            });
        });
        
        userEmails.forEach(email -> {
            notificationService.addNotification(
                    email,
                    title,
                    message,
                    "ADMIN_BROADCAST"
            );
        });
        
        return new MessageResponse(String.format("Broadcast sent to %d active booking users", userEmails.size()));
    }
}