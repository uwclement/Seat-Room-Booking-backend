package com.auca.library.service;

import com.auca.library.dto.request.*;
import com.auca.library.dto.response.*;
import com.auca.library.exception.ResourceNotFoundException;
import com.auca.library.exception.BookingConflictException;
import com.auca.library.model.*;
import com.auca.library.repository.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class RoomBookingService {
    
    @Autowired private RoomBookingRepository roomBookingRepository;
    @Autowired private RoomRepository roomRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private BookingParticipantRepository participantRepository;
    @Autowired private NotificationService notificationService;
    @Autowired private RoomAvailabilityService roomAvailabilityService;
    @Autowired private BookingValidationService bookingValidationService;
    @Autowired private RecurringBookingService recurringBookingService;
    @Autowired private EquipmentRepository equipmentRepository;

    // Create new booking
    
    @Transactional
    public RoomBookingResponse createBooking(RoomBookingRequest request, String userEmail) {
        User user = findUserByEmail(userEmail);
        Room room = findRoomById(request.getRoomId());
        
        // Validate booking request
        bookingValidationService.validateBookingRequest(request, user, room);
        
        // Check for conflicts/all already booked 
        RoomAvailabilityResponse availability = roomAvailabilityService.getRoomAvailability(room.getId());
        if (!isTimeSlotAvailable(availability, request.getStartTime(), request.getEndTime())) {
    throw new BookingConflictException("Room is not available for the requested time");
}
        
        // Create booking
        RoomBooking booking = new RoomBooking();
        booking.setRoom(room);
        booking.setUser(user);
        booking.setTitle(request.getTitle());
        booking.setDescription(request.getDescription());
        booking.setStartTime(request.getStartTime());
        booking.setEndTime(request.getEndTime());
        booking.setMaxParticipants(Math.min(request.getMaxParticipants(), room.getCapacity()));
        booking.setPublic(request.isPublic()); 
        booking.setAllowJoining(request.isAllowJoining());
        booking.setRequiresCheckIn(request.isRequiresCheckIn());
        booking.setReminderEnabled(request.isReminderEnabled());
        
        // Set approval requirement
        booking.setRequiresApproval(room.isRequiresApproval());
        booking.setStatus(room.isRequiresApproval() ? 
                         RoomBooking.BookingStatus.PENDING : 
                         RoomBooking.BookingStatus.CONFIRMED);
        
        // Handle equipment requests
        if (request.getRequestedEquipmentIds() != null && !request.getRequestedEquipmentIds().isEmpty()) {
            Set<Equipment> equipment = request.getRequestedEquipmentIds().stream()
                    .map(this::findEquipmentById)
                    .collect(Collectors.toSet());
            booking.setRequestedEquipment(equipment);
        }
        
        booking = roomBookingRepository.save(booking);
        
        // Handle recurring booking
        if (request.isRecurring() && request.getRecurringDetails() != null) {
            recurringBookingService.createRecurringSeries(booking, request.getRecurringDetails());
        }
        
        // Send invitations
        if (request.getInvitedUserEmails() != null || request.getInvitedUserIds() != null) {
            inviteParticipants(booking, request.getInvitedUserEmails(), request.getInvitedUserIds());
        }
        
        // Send notifications
        sendBookingNotifications(booking);
        
        return mapToResponse(booking);
    }
    
    // Update booking
    @Transactional
    public RoomBookingResponse updateBooking(Long bookingId, BookingUpdateRequest request, String userEmail) {
        RoomBooking booking = findBookingById(bookingId);
        User user = findUserByEmail(userEmail);
        
        // Check permissions
        if (!canUserEditBooking(booking, user)) {
            throw new SecurityException("User cannot edit this booking");
        }
        
        // Validate time changes
        if (request.getStartTime() != null || request.getEndTime() != null) {
            LocalDateTime newStartTime = request.getStartTime() != null ? request.getStartTime() : booking.getStartTime();
            LocalDateTime newEndTime = request.getEndTime() != null ? request.getEndTime() : booking.getEndTime();
            
            if (hasConflictExcluding(booking.getRoom(), newStartTime, newEndTime, bookingId)) {
                throw new BookingConflictException("Updated time conflicts with existing booking");
            }
        }
        
        // Update fields
        if (request.getTitle() != null) booking.setTitle(request.getTitle());
        if (request.getDescription() != null) booking.setDescription(request.getDescription());
        if (request.getStartTime() != null) booking.setStartTime(request.getStartTime());
        if (request.getEndTime() != null) booking.setEndTime(request.getEndTime());
        if (request.getMaxParticipants() != null) {
            booking.setMaxParticipants(Math.min(request.getMaxParticipants(), booking.getRoom().getCapacity()));
        }
        if (request.getIsPublic() != null)booking.setPublic(request.getIsPublic());
        if (request.getAllowJoining() != null) booking.setAllowJoining(request.getAllowJoining());
        if (request.getReminderEnabled() != null) booking.setReminderEnabled(request.getReminderEnabled());
        
        // Update equipment
        if (request.getRequestedEquipmentIds() != null) {
            Set<Equipment> equipment = request.getRequestedEquipmentIds().stream()
                    .map(this::findEquipmentById)
                    .collect(Collectors.toSet());
            booking.setRequestedEquipment(equipment);
        }
        
        booking = roomBookingRepository.save(booking);
        
        // Notify participants of changes
        notifyParticipantsOfUpdate(booking);
        
        return mapToResponse(booking);
    }
    
    // Cancel booking
    @Transactional
    public MessageResponse cancelBooking(Long bookingId, String userEmail) {
        RoomBooking booking = findBookingById(bookingId);
        User user = findUserByEmail(userEmail);
        
        if (!canUserCancelBooking(booking, user)) {
            throw new SecurityException("User cannot cancel this booking");
        }
        
        booking.setStatus(RoomBooking.BookingStatus.CANCELLED);
        roomBookingRepository.save(booking);
        
        // Notify participants
        notifyParticipantsOfCancellation(booking);
        
        // Process waitlist
        processWaitlistForCancellation(booking);
        
        return new MessageResponse("Booking cancelled successfully");
    }
    
    // Check in to booking
    @Transactional
    public MessageResponse checkInToBooking(Long bookingId, String userEmail) {
        RoomBooking booking = findBookingById(bookingId);
        User user = findUserByEmail(userEmail);
        
        if (!booking.canCheckIn()) {
            throw new IllegalStateException("Cannot check in to this booking at this time");
        }
        
        // Check if user is the organizer
        if (booking.getUser().equals(user)) {
            booking.setCheckedInAt(LocalDateTime.now());
            booking.setStatus(RoomBooking.BookingStatus.CHECKED_IN);
        } else {
            // Check if user is a participant
            BookingParticipant participant = participantRepository.findByBookingAndUser(booking, user)
                    .orElseThrow(() -> new SecurityException("User is not authorized to check in to this booking"));
            
            if (participant.getStatus() != BookingParticipant.ParticipantStatus.ACCEPTED) {
                throw new IllegalStateException("Participant must accept invitation before checking in");
            }
            
            participant.setCheckedInAt(LocalDateTime.now());
            participantRepository.save(participant);
        }
        
        roomBookingRepository.save(booking);
        
        return new MessageResponse("Checked in successfully");
    }
    
    // Join public booking
    @Transactional
    public MessageResponse joinBooking(JoinBookingRequest request, String userEmail) {
        RoomBooking booking = findBookingById(request.getBookingId());
        User user = findUserByEmail(userEmail);
        
        if (!booking.isPublic() || !booking.isAllowJoining()) {
            throw new IllegalStateException("This booking is not open for joining");
        }
        
        Long acceptedCount = participantRepository.countAcceptedParticipants(booking);
        if (acceptedCount >= booking.getMaxParticipants() - 1) { // -1 for organizer
            throw new IllegalStateException("Booking is at full capacity");
        }
        
        // Check if user is already a participant
        if (participantRepository.findByBookingAndUser(booking, user).isPresent()) {
            throw new IllegalStateException("User is already a participant in this booking");
        }
        
        // Create participant record
        BookingParticipant participant = new BookingParticipant();
        participant.setBooking(booking);
        participant.setUser(user);
        participant.setStatus(BookingParticipant.ParticipantStatus.ACCEPTED);
        participant.setRespondedAt(LocalDateTime.now());
        participantRepository.save(participant);
        
        // Notify organizer
        notificationService.addNotification(
                booking.getUser().getEmail(),
                "New Participant Joined",
                String.format("%s has joined your booking: %s", user.getFullName(), booking.getTitle()),
                "BOOKING_UPDATE"
        );
        
        return new MessageResponse("Successfully joined the booking");
    }
    
    // Get user's bookings
    public List<RoomBookingResponse> getUserBookings(String userEmail) {
        User user = findUserByEmail(userEmail);
        List<RoomBooking.BookingStatus> activeStatuses = List.of(
                RoomBooking.BookingStatus.PENDING,
                RoomBooking.BookingStatus.CONFIRMED,
                RoomBooking.BookingStatus.CHECKED_IN
        );
        
        List<RoomBooking> bookings = roomBookingRepository.findByUserAndStatusIn(user, activeStatuses);
        
        // Include bookings where user is a participant
        List<BookingParticipant> participations = participantRepository.findByUserAndStatusIn(
                user, List.of(BookingParticipant.ParticipantStatus.ACCEPTED)
        );
        
        participations.forEach(p -> {
            if (activeStatuses.contains(p.getBooking().getStatus())) {
                bookings.add(p.getBooking());
            }
        });
        
        return bookings.stream().distinct().map(this::mapToResponse).collect(Collectors.toList());
    }
    
    // Get booking history
    public BookingHistoryResponse getUserBookingHistory(String userEmail, Pageable pageable) {
        User user = findUserByEmail(userEmail);
        List<RoomBooking> bookings = roomBookingRepository.findUserBookingHistory(user);
        
        BookingHistoryResponse response = new BookingHistoryResponse();
        response.setBookings(bookings.stream().map(this::mapToResponse).collect(Collectors.toList()));
        response.setStatistics(calculateUserBookingStatistics(user));
        response.setTotalBookings(bookings.size());
        
        return response;
    }
    
    // Get joinable bookings
    public List<RoomBookingResponse> getJoinableBookings() {
        List<RoomBooking> bookings = roomBookingRepository.findJoinableBookings(LocalDateTime.now());
        return bookings.stream().map(this::mapToResponse).collect(Collectors.toList());
    }
    
    // Helper methods
    private boolean hasConflict(Room room, LocalDateTime startTime, LocalDateTime endTime) {
        return roomBookingRepository.countConflictingBookings(room, startTime, endTime) > 0;
    }
    
    private boolean hasConflictExcluding(Room room, LocalDateTime startTime, LocalDateTime endTime, Long excludeBookingId) {
        List<RoomBooking> conflicts = roomBookingRepository.findActiveBookingsForRoom(room, startTime, endTime);
        return conflicts.stream().anyMatch(b -> !b.getId().equals(excludeBookingId));
    }
    
    private void inviteParticipants(RoomBooking booking, List<String> emails, List<Long> userIds) {
        // Invite by email
        if (emails != null) {
            emails.forEach(email -> {
                User user = userRepository.findByEmail(email).orElse(null);
                if (user != null) {
                    createParticipantInvitation(booking, user);
                }
            });
        }
        
        // Invite by user ID
        if (userIds != null) {
            userIds.forEach(userId -> {
                User user = userRepository.findById(userId).orElse(null);
                if (user != null) {
                    createParticipantInvitation(booking, user);
                }
            });
        }
    }
    
    private void createParticipantInvitation(RoomBooking booking, User user) {
        BookingParticipant participant = new BookingParticipant();
        participant.setBooking(booking);
        participant.setUser(user);
        participant.setStatus(BookingParticipant.ParticipantStatus.INVITED);
        participantRepository.save(participant);
        
        // Send invitation notification
        notificationService.addNotification(
                user.getEmail(),
                "Room Booking Invitation",
                String.format("You've been invited to join: %s", booking.getTitle()),
                "BOOKING_INVITATION"
        );
    }
    
    private void sendBookingNotifications(RoomBooking booking) {
        if (booking.isRequiresApproval()) {
            // Notify admins for approval
            notificationService.addNotification(
                    "clement1@gmail.com", // Or get admin emails from role
                    "Booking Approval Required",
                    String.format("New booking request requires approval: %s", booking.getTitle()),
                    "APPROVAL_REQUIRED"
            );
        } else {
            // Confirm booking to user
            notificationService.addNotification(
                    booking.getUser().getEmail(),
                    "Booking Confirmed",
                    String.format("Your booking has been confirmed: %s", booking.getTitle()),
                    "BOOKING_CONFIRMED"
            );
        }
    }
    
    private boolean canUserEditBooking(RoomBooking booking, User user) {
        return booking.getUser().equals(user) || user.getRoles().stream()
                .anyMatch(role -> role.getName() == Role.ERole.ROLE_ADMIN);
    }
    
    private boolean canUserCancelBooking(RoomBooking booking, User user) {
        return canUserEditBooking(booking, user);
    }
    
    private void notifyParticipantsOfUpdate(RoomBooking booking) {
        booking.getParticipants().forEach(participant -> {
            if (participant.getStatus() == BookingParticipant.ParticipantStatus.ACCEPTED) {
                notificationService.addNotification(
                        participant.getUser().getEmail(),
                        "Booking Updated",
                        String.format("The booking '%s' has been updated", booking.getTitle()),
                        "BOOKING_UPDATE"
                );
            }
        });
    }
    
    private void notifyParticipantsOfCancellation(RoomBooking booking) {
        booking.getParticipants().forEach(participant -> {
            notificationService.addNotification(
                    participant.getUser().getEmail(),
                    "Booking Cancelled",
                    String.format("The booking '%s' has been cancelled", booking.getTitle()),
                    "BOOKING_CANCELLED"
            );
        });
    }
    

    // In RoomBookingService
public List<InvitationResponse> getUserPendingInvitations(String userEmail) {
    User user = findUserByEmail(userEmail);
    
    List<BookingParticipant> pendingInvitations = participantRepository.findByUserAndStatusIn(
        user, List.of(BookingParticipant.ParticipantStatus.INVITED)
    );
    
    return pendingInvitations.stream()
            .map(this::mapToInvitationResponse)
            .collect(Collectors.toList());
}

private InvitationResponse mapToInvitationResponse(BookingParticipant participant) {
    InvitationResponse response = new InvitationResponse();
    response.setParticipantId(participant.getId());
    response.setBooking(mapToResponse(participant.getBooking()));
    response.setInvitedAt(participant.getInvitedAt());
    response.setInviterName(participant.getBooking().getUser().getFullName());
    return response;
}

    private void processWaitlistForCancellation(RoomBooking booking) {
        // This will be implemented in WaitlistService
        // waitlistService.processWaitlistForAvailableSlot(booking.getRoom(), booking.getStartTime(), booking.getEndTime());
    }
    
    private BookingHistoryResponse.BookingStatistics calculateUserBookingStatistics(User user) {
        // Implementation for user statistics calculation
        BookingHistoryResponse.BookingStatistics stats = new BookingHistoryResponse.BookingStatistics();
        // ... calculate statistics
        return stats;
    }
    
    
    private User findUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + email));
    }
    
    private Room findRoomById(Long roomId) {
        return roomRepository.findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found: " + roomId));
    }
    
    private RoomBooking findBookingById(Long bookingId) {
        return roomBookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found: " + bookingId));
    }
    
    private Equipment findEquipmentById(Long equipmentId) {
        return equipmentRepository.findById(equipmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Equipment not found: " + equipmentId));
    }

   private boolean isTimeSlotAvailable(RoomAvailabilityResponse availability, LocalDateTime startTime, LocalDateTime endTime) {
    return availability.getAvailableSlots().stream()
        .anyMatch(slot -> 
            !slot.getStartTime().isAfter(startTime) && 
            !slot.getEndTime().isBefore(endTime)
        );
}


//Room response mapper
private RoomResponse mapRoomToResponse(Room room) {
    RoomResponse response = new RoomResponse();
    response.setId(room.getId());
    response.setRoomNumber(room.getRoomNumber());
    response.setName(room.getName());
    response.setDescription(room.getDescription());
    response.setCategory(room.getCategory());
    response.setCapacity(room.getCapacity());
    response.setMaxBookingHours(room.getMaxBookingHours());
    response.setMaxBookingsPerDay(room.getMaxBookingsPerDay());
    response.setAdvanceBookingDays(room.getAdvanceBookingDays());
    response.setAvailable(room.isAvailable());
    response.setRequiresBooking(room.requiresBooking());
    
    // Location details
    response.setBuilding(room.getBuilding());
    response.setFloor(room.getFloor());
    response.setDepartment(room.getDepartment());
    
    // Equipment mapping
    if (room.getEquipment() != null) {
        response.setEquipment(room.getEquipment().stream()
            .map(this::mapEquipmentToResponse)
            .collect(Collectors.toSet()));
    }
    
    // Maintenance info
    response.setMaintenanceStart(room.getMaintenanceStart());
    response.setMaintenanceEnd(room.getMaintenanceEnd());
    response.setMaintenanceNotes(room.getMaintenanceNotes());
    response.setUnderMaintenance(room.isUnderMaintenance());
    
    // Approval settings
    response.setRequiresApproval(room.isRequiresApproval());
    
    // Timestamps
    response.setCreatedAt(room.getCreatedAt());
    response.setUpdatedAt(room.getUpdatedAt());
    
    return response;
}

// User response mapper
private UserResponse mapUserToResponse(User user) {
    UserResponse response = new UserResponse();
    response.setId(user.getId());
    response.setFullName(user.getFullName());
    response.setEmail(user.getEmail());
    response.setIdentifier(user.getIdentifier()); // Make sure User entity has this field
    response.setEmailVerified(user.isEmailVerified()); // Make sure User entity has this field
    
    // Map roles to List<String>
    if (user.getRoles() != null) {
        response.setRoles(user.getRoles().stream()
            .map(role -> role.getName().name())
            .collect(Collectors.toList()));
    }
    
    return response;
}

// 4. Participant response mapper
private BookingParticipantResponse mapParticipantToResponse(BookingParticipant participant) {
    BookingParticipantResponse response = new BookingParticipantResponse();
    response.setId(participant.getId());
    response.setUser(mapUserToResponse(participant.getUser()));

    response.setStatus(convertParticipantStatus(participant.getStatus()));
    
    response.setInvitedAt(participant.getInvitedAt());
    response.setRespondedAt(participant.getRespondedAt());
    response.setCheckedInAt(participant.getCheckedInAt());
    response.setNotificationSent(participant.isNotificationSent());
    
    return response;
}

private BookingParticipantResponse.ParticipantStatus convertParticipantStatus(BookingParticipant.ParticipantStatus entityStatus) {
    switch (entityStatus) {
        case INVITED:
            return BookingParticipantResponse.ParticipantStatus.INVITED;
        case ACCEPTED:
            return BookingParticipantResponse.ParticipantStatus.ACCEPTED;
        case DECLINED:
            return BookingParticipantResponse.ParticipantStatus.DECLINED;
        case REMOVED:
            return BookingParticipantResponse.ParticipantStatus.REMOVED;
        default:
            return BookingParticipantResponse.ParticipantStatus.INVITED;
    }
}

// 5. Equipment response mapper
private EquipmentResponse mapEquipmentToResponse(Equipment equipment) {
    EquipmentResponse response = new EquipmentResponse();
    response.setId(equipment.getId());
    response.setName(equipment.getName());
    response.setDescription(equipment.getDescription());
    response.setAvailable(equipment.isAvailable());
    
    return response;
}

// 6. Recurring booking series response mapper (if needed)
private RecurringBookingSeriesResponse mapRecurringSeriesToResponse(RecurringBookingSeries series) {
    if (series == null) return null;
    
    RecurringBookingSeriesResponse response = new RecurringBookingSeriesResponse();
    response.setId(series.getId());
    response.setUser(mapUserToResponse(series.getUser()));
    response.setRoom(mapRoomToResponse(series.getRoom()));
    response.setTitle(series.getTitle());
    response.setDescription(series.getDescription());
    
    // Convert entity enum to DTO enum
    response.setRecurrenceType(convertRecurrenceType(series.getRecurrenceType()));
    
    response.setRecurrenceInterval(series.getRecurrenceInterval());
     // Convert Set<DayOfWeek> to Set<String>
    if (series.getDaysOfWeek() != null) {
        response.setDaysOfWeek(series.getDaysOfWeek().stream()
            .map(DayOfWeek::name) // Convert enum to string
            .collect(Collectors.toSet()));
    }
    response.setStartTime(series.getStartTime().toString()); // Convert LocalTime to String
    response.setEndTime(series.getEndTime().toString()); // Convert LocalTime to String
    response.setSeriesStartDate(series.getSeriesStartDate());
    response.setSeriesEndDate(series.getSeriesEndDate());
    response.setActive(series.isActive());
     response.setTotalBookings(calculateTotalBookings(series));
    response.setCompletedBookings(calculateCompletedBookings(series));
    response.setLastGeneratedDate(series.getLastGeneratedDate());
    response.setCreatedAt(series.getCreatedAt());
    
    return response;
}

// Helper method to convert entity enum to DTO enum for RecurrenceType
private RecurringBookingSeriesResponse.RecurrenceType convertRecurrenceType(RecurringBookingSeries.RecurrenceType entityType) {
    switch (entityType) {
        case DAILY:
            return RecurringBookingSeriesResponse.RecurrenceType.DAILY;
        case WEEKLY:
            return RecurringBookingSeriesResponse.RecurrenceType.WEEKLY;
        case MONTHLY:
            return RecurringBookingSeriesResponse.RecurrenceType.MONTHLY;
        case CUSTOM:
            return RecurringBookingSeriesResponse.RecurrenceType.CUSTOM;
        default:
            return RecurringBookingSeriesResponse.RecurrenceType.WEEKLY;
    }
}


// Optimized method to get recurring series statistics using single query
private RecurringSeriesStats getRecurringSeriesStats(RecurringBookingSeries series) {
    try {
        return roomBookingRepository.getRecurringSeriesStats(series);
    } catch (Exception e) {
        // If repository method doesn't exist, return default stats
        return new RecurringSeriesStats(0L, 0L, 0L, 0L);
    }
}

// Helper method to calculate total bookings for a series (fallback)
private Integer calculateTotalBookings(RecurringBookingSeries series) {
    RecurringSeriesStats stats = getRecurringSeriesStats(series);
    return stats.getTotalBookings().intValue();
}

// Helper method to calculate completed bookings for a series (fallback)
private Integer calculateCompletedBookings(RecurringBookingSeries series) {
    RecurringSeriesStats stats = getRecurringSeriesStats(series);
    return stats.getCompletedBookings().intValue();
}

    private RoomBookingResponse mapToResponse(RoomBooking booking) {
    RoomBookingResponse response = new RoomBookingResponse();
    response.setId(booking.getId());
    response.setTitle(booking.getTitle());
    response.setDescription(booking.getDescription());
    response.setStartTime(booking.getStartTime());
    response.setEndTime(booking.getEndTime());
    response.setStatus(booking.getStatus());
    response.setMaxParticipants(booking.getMaxParticipants());
    response.setPublic(booking.isPublic());
    response.setAllowJoining(booking.isAllowJoining());
    response.setRequiresCheckIn(booking.isRequiresCheckIn());
    response.setCheckedInAt(booking.getCheckedInAt());
    response.setReminderEnabled(booking.isReminderEnabled());
    response.setReminderSentAt(booking.getReminderSentAt());
    response.setRequiresApproval(booking.isRequiresApproval());
    response.setCreatedAt(booking.getCreatedAt());
    response.setUpdatedAt(booking.getUpdatedAt());
    
    // Map related entities (you'll need these response classes)
    response.setRoom(mapRoomToResponse(booking.getRoom()));
    response.setUser(mapUserToResponse(booking.getUser()));
    
    // Map participants
    response.setParticipants(booking.getParticipants().stream()
        .map(this::mapParticipantToResponse)
        .collect(Collectors.toList()));
    
    // Map equipment
    response.setRequestedEquipment(booking.getRequestedEquipment().stream()
        .map(this::mapEquipmentToResponse)
        .collect(Collectors.toSet()));
    
    // Set helper fields
    response.setCanCheckIn(booking.canCheckIn());
    response.setOverdue(booking.isOverdue());
    response.setCheckedInCount(booking.getCheckedInCount());
    response.setTotalParticipants(booking.getParticipants().size() + 1); // +1 for organizer
    
    return response;
}



// 1. Get booking by ID with user validation
public RoomBookingResponse getBookingById(Long bookingId, String userEmail) {
    RoomBooking booking = findBookingById(bookingId);
    User user = findUserByEmail(userEmail);
    
    // Check if user can view this booking
    if (!canUserViewBooking(booking, user)) {
        throw new SecurityException("User cannot view this booking");
    }
    
    return mapToResponse(booking);
}

// 2. Get weekly availability
public WeeklyRoomAvailabilityResponse getWeeklyAvailability(Long roomId, LocalDateTime weekStart, String userEmail) {
    // Validate user has permission to view this room
    User user = findUserByEmail(userEmail);
    Room room = findRoomById(roomId);
    
    return roomAvailabilityService.getWeeklyRoomAvailability(roomId, weekStart);
}

// 3. Invite participants
public MessageResponse inviteParticipants(Long bookingId, InviteParticipantsRequest request, String userEmail) {
    RoomBooking booking = findBookingById(bookingId);
    User user = findUserByEmail(userEmail);
    
    if (!canUserEditBooking(booking, user)) {
        throw new SecurityException("User cannot invite participants to this booking");
    }
    
    // Invite by email
    if (request.getInvitedEmails() != null) {
        inviteParticipants(booking, request.getInvitedEmails(), null);
    }
    
    // Invite by user ID
    if (request.getInvitedUserIds() != null) {
        inviteParticipants(booking, null, request.getInvitedUserIds());
    }
    
    return new MessageResponse("Participants invited successfully");
}

// 4. Respond to invitation
public MessageResponse respondToInvitation(Long bookingId, Long participantId, InvitationResponseRequest request, String userEmail) {
    RoomBooking booking = findBookingById(bookingId);
    User user = findUserByEmail(userEmail);
    
    BookingParticipant participant = participantRepository.findById(participantId)
            .orElseThrow(() -> new ResourceNotFoundException("Participant not found"));
    
    if (!participant.getUser().equals(user)) {
        throw new SecurityException("User cannot respond to this invitation");
    }
    
    if (request.getAccepted()) {
        participant.setStatus(BookingParticipant.ParticipantStatus.ACCEPTED);
    } else {
        participant.setStatus(BookingParticipant.ParticipantStatus.DECLINED);
    }
    
    participant.setRespondedAt(LocalDateTime.now());
    participantRepository.save(participant);
    
    return new MessageResponse("Invitation response recorded");
}

// 5. Remove participant
public MessageResponse removeParticipant(Long bookingId, Long participantId, String userEmail) {
    RoomBooking booking = findBookingById(bookingId);
    User user = findUserByEmail(userEmail);
    
    if (!canUserEditBooking(booking, user)) {
        throw new SecurityException("User cannot remove participants from this booking");
    }
    
    BookingParticipant participant = participantRepository.findById(participantId)
            .orElseThrow(() -> new ResourceNotFoundException("Participant not found"));
    
    participant.setStatus(BookingParticipant.ParticipantStatus.REMOVED);
    participantRepository.save(participant);
    
    return new MessageResponse("Participant removed successfully");
}

// 6. Get recurring series
public RecurringBookingSeriesResponse getRecurringSeries(Long bookingId, String userEmail) {
    RoomBooking booking = findBookingById(bookingId);
    User user = findUserByEmail(userEmail);
    
    if (!canUserViewBooking(booking, user)) {
        throw new SecurityException("User cannot view this booking's recurring series");
    }
    
    if (booking.getRecurringBookingSeries() == null) {
        throw new IllegalStateException("This booking is not part of a recurring series");
    }
    
    return mapRecurringSeriesToResponse(booking.getRecurringBookingSeries());
}

// 7. Cancel recurring series
public MessageResponse cancelRecurringSeries(Long bookingId, String userEmail) {
    RoomBooking booking = findBookingById(bookingId);
    User user = findUserByEmail(userEmail);
    
    if (!canUserEditBooking(booking, user)) {
        throw new SecurityException("User cannot cancel this recurring series");
    }
    
    if (booking.getRecurringBookingSeries() == null) {
        throw new IllegalStateException("This booking is not part of a recurring series");
    }
    
    // Delegate to RecurringBookingService
    recurringBookingService.cancelRecurringSeries(
        booking.getRecurringBookingSeries().getId(), 
        userEmail
    );
    
    return new MessageResponse("Recurring series cancelled successfully");
}

// 8. Search bookings
public List<RoomBookingResponse> searchBookings(BookingSearchRequest searchRequest, String userEmail) {
    User user = findUserByEmail(userEmail);
    
    // Build search criteria - this is a simplified implementation
    List<RoomBooking> bookings = roomBookingRepository.findAll(); // You'll need a proper search method
    
    // Apply filters
    if (searchRequest.getKeyword() != null) {
        bookings = bookings.stream()
            .filter(b -> b.getTitle().toLowerCase().contains(searchRequest.getKeyword().toLowerCase()) ||
                        b.getDescription().toLowerCase().contains(searchRequest.getKeyword().toLowerCase()))
            .collect(Collectors.toList());
    }
    
    if (searchRequest.getRoomId() != null) {
        bookings = bookings.stream()
            .filter(b -> b.getRoom().getId().equals(searchRequest.getRoomId()))
            .collect(Collectors.toList());
    }
    
    // Filter by user access
    bookings = bookings.stream()
        .filter(b -> canUserViewBooking(b, user))
        .collect(Collectors.toList());
    
    return bookings.stream().map(this::mapToResponse).collect(Collectors.toList());
}

// 9. Quick book
public RoomBookingResponse quickBook(Long roomId, int durationHours, String userEmail) {
    User user = findUserByEmail(userEmail);
    Room room = findRoomById(roomId);
    
    // Find next available slot
    LocalDateTime now = LocalDateTime.now();
    LocalDateTime proposedStart = now.plusMinutes(15); // 15 minutes from now
    LocalDateTime proposedEnd = proposedStart.plusHours(durationHours);
    
    // Check availability
    if (hasConflict(room, proposedStart, proposedEnd)) {
        throw new BookingConflictException("No immediate availability for quick booking");
    }
    
    // Create quick booking
    RoomBookingRequest request = new RoomBookingRequest();
    request.setRoomId(roomId);
    request.setTitle("Quick Booking");
    request.setStartTime(proposedStart);
    request.setEndTime(proposedEnd);
    request.setMaxParticipants(1);
    
    return createBooking(request, userEmail);
}

// 10. Extend booking
public RoomBookingResponse extendBooking(Long bookingId, ExtendBookingRequest request, String userEmail) {
    RoomBooking booking = findBookingById(bookingId);
    User user = findUserByEmail(userEmail);
    
    if (!canUserEditBooking(booking, user)) {
        throw new SecurityException("User cannot extend this booking");
    }
    
    LocalDateTime newEndTime = booking.getEndTime().plusHours(request.getAdditionalHours());
    
    // Check for conflicts
    if (hasConflictExcluding(booking.getRoom(), booking.getStartTime(), newEndTime, bookingId)) {
        throw new BookingConflictException("Cannot extend booking due to conflicts");
    }
    
    booking.setEndTime(newEndTime);
    roomBookingRepository.save(booking);
    
    return mapToResponse(booking);
}

// 11. Get user stats
public UserBookingStatsResponse getUserStats(String userEmail, int weeks) {
    User user = findUserByEmail(userEmail);
    
    LocalDateTime cutoffDate = LocalDateTime.now().minusWeeks(weeks);
    List<RoomBooking> recentBookings = roomBookingRepository.findByUserAndStartTimeAfter(user, cutoffDate);
    
    UserBookingStatsResponse stats = new UserBookingStatsResponse();
    stats.setTotalBookings(recentBookings.size());
    stats.setCompletedBookings((int) recentBookings.stream()
        .filter(b -> b.getStatus() == RoomBooking.BookingStatus.COMPLETED)
        .count());
    stats.setCancelledBookings((int) recentBookings.stream()
        .filter(b -> b.getStatus() == RoomBooking.BookingStatus.CANCELLED)
        .count());
    
    // Calculate averages, most used rooms, etc.
    
    return stats;
}

// Helper method for viewing permissions
private boolean canUserViewBooking(RoomBooking booking, User user) {
    // User can view if they're the owner, participant, or admin
    if (booking.getUser().equals(user)) {
        return true;
    }
    
    // Check if user is a participant
    boolean isParticipant = booking.getParticipants().stream()
        .anyMatch(p -> p.getUser().equals(user) && 
                      p.getStatus() == BookingParticipant.ParticipantStatus.ACCEPTED);
    
    if (isParticipant) {
        return true;
    }
    
    // Check if user is admin
    return user.getRoles().stream()
        .anyMatch(role -> role.getName() == Role.ERole.ROLE_ADMIN);
}


// ========== ROOM DISCOVERY METHODS ==========

public List<RoomResponse> getAvailableRooms() {
    // Use the simpler method and filter in Java
    List<Room> allRooms = roomRepository.findByAvailableTrue();
    return allRooms.stream()
            .filter(Room::requiresBooking) // This uses the helper method in Room entity
            .filter(room -> !room.isUnderMaintenance())
            .map(this::mapRoomToResponse)
            .collect(Collectors.toList());
}

public RoomResponse getRoomById(Long roomId) {
    Room room = findRoomById(roomId);
    if (!room.isAvailable() || room.isUnderMaintenance()) {
        throw new IllegalStateException("Room is not available for booking");
    }
    return mapRoomToResponse(room);
}

public List<RoomResponse> getRoomsByCategory(String category) {
    try {
        RoomCategory roomCategory = RoomCategory.valueOf(category.toUpperCase());
        List<Room> rooms = roomRepository.findByCategoryAndAvailableTrue(roomCategory);
        return rooms.stream()
                .filter(Room::requiresBooking)
                .filter(room -> !room.isUnderMaintenance())
                .map(this::mapRoomToResponse)
                .collect(Collectors.toList());
    } catch (IllegalArgumentException e) {
        throw new IllegalArgumentException("Invalid room category: " + category);
    }
}
public List<RoomResponse> searchAvailableRooms(RoomSearchRequest searchRequest) {
    List<Room> rooms = roomRepository.findAll(); // You'll need a proper search method
    
    // Apply filters
    rooms = rooms.stream()
            .filter(Room::isAvailable)
            .filter(room -> !room.isUnderMaintenance())
            .filter(Room::requiresBooking)
            .collect(Collectors.toList());
    
    // Apply search filters
    if (searchRequest.getKeyword() != null && !searchRequest.getKeyword().isEmpty()) {
        rooms = rooms.stream()
                .filter(room -> room.getName().toLowerCase().contains(searchRequest.getKeyword().toLowerCase()) ||
                               room.getDescription().toLowerCase().contains(searchRequest.getKeyword().toLowerCase()))
                .collect(Collectors.toList());
    }
    
    if (searchRequest.getCategory() != null) {
        try {
            RoomCategory category = RoomCategory.valueOf(searchRequest.getCategory().toUpperCase());
            rooms = rooms.stream()
                    .filter(room -> room.getCategory() == category)
                    .collect(Collectors.toList());
        } catch (IllegalArgumentException e) {
            // Invalid category, return empty list
            return new ArrayList<>();
        }
    }
    
    if (searchRequest.getMinCapacity() != null) {
        rooms = rooms.stream()
                .filter(room -> room.getCapacity() >= searchRequest.getMinCapacity())
                .collect(Collectors.toList());
    }
    
    if (searchRequest.getMaxCapacity() != null) {
        rooms = rooms.stream()
                .filter(room -> room.getCapacity() <= searchRequest.getMaxCapacity())
                .collect(Collectors.toList());
    }
    
    if (searchRequest.getBuilding() != null) {
        rooms = rooms.stream()
                .filter(room -> room.getBuilding().equalsIgnoreCase(searchRequest.getBuilding()))
                .collect(Collectors.toList());
    }
    
    if (searchRequest.getFloor() != null) {
        rooms = rooms.stream()
                .filter(room -> room.getFloor().equalsIgnoreCase(searchRequest.getFloor()))
                .collect(Collectors.toList());
    }
    
    if (searchRequest.getDepartment() != null) {
        rooms = rooms.stream()
                .filter(room -> room.getDepartment().equalsIgnoreCase(searchRequest.getDepartment()))
                .collect(Collectors.toList());
    }
    
    // Check time availability if provided
    if (searchRequest.getStartTime() != null && searchRequest.getEndTime() != null) {
        rooms = rooms.stream()
                .filter(room -> !hasConflict(room, searchRequest.getStartTime(), searchRequest.getEndTime()))
                .collect(Collectors.toList());
    }
    
    // Filter by equipment if provided
    if (searchRequest.getEquipmentIds() != null && !searchRequest.getEquipmentIds().isEmpty()) {
        rooms = rooms.stream()
                .filter(room -> room.getEquipment().stream()
                        .map(Equipment::getId)
                        .collect(Collectors.toSet())
                        .containsAll(searchRequest.getEquipmentIds()))
                .collect(Collectors.toList());
    }
    
    return rooms.stream()
            .map(this::mapRoomToResponse)
            .collect(Collectors.toList());
}

public List<String> getAllBuildings() {
    // Simple approach - get all available rooms and extract unique buildings
    List<Room> rooms = roomRepository.findByAvailableTrue();
    return rooms.stream()
            .filter(Room::requiresBooking)
            .map(Room::getBuilding)
            .filter(building -> building != null && !building.trim().isEmpty())
            .distinct()
            .sorted()
            .collect(Collectors.toList());
}

public List<String> getFloorsInBuilding(String building) {
    // Simple approach - get rooms in building and extract unique floors
    List<Room> rooms = roomRepository.findByBuildingAndAvailable(building, true);
    return rooms.stream()
            .filter(Room::requiresBooking)
            .map(Room::getFloor)
            .filter(floor -> floor != null && !floor.trim().isEmpty())
            .distinct()
            .sorted()
            .collect(Collectors.toList());
}

public List<EquipmentResponse> getAvailableEquipment() {
    List<Equipment> equipment = equipmentRepository.findByAvailable(true);
    return equipment.stream()
            .map(this::mapEquipmentToResponse)
            .collect(Collectors.toList());
}

public List<String> getRoomCategories() {
    return Arrays.stream(RoomCategory.values())
            .map(Enum::name)
            .collect(Collectors.toList());
}

// ========== AVAILABILITY METHODS ==========

public List<RoomResponse> getRoomsAvailableNow(int durationHours) {
    LocalDateTime now = LocalDateTime.now();
    LocalDateTime endTime = now.plusHours(durationHours);
    
    List<Room> rooms = roomRepository.findByAvailableTrue();
    
    return rooms.stream()
            .filter(Room::requiresBooking)
            .filter(room -> !room.isUnderMaintenance())
            .filter(room -> !hasConflict(room, now, endTime))
            .map(this::mapRoomToResponse)
            .collect(Collectors.toList());
}

public NextAvailableSlotResponse getNextAvailableSlot(Long roomId, int durationHours, LocalDateTime fromTime) {
    Room room = findRoomById(roomId);
    
    if (!room.isAvailable() || room.isUnderMaintenance()) {
        throw new IllegalStateException("Room is not available for booking");
    }
    
    // Simple implementation - you may want to make this more sophisticated
    LocalDateTime searchTime = fromTime;
    LocalDateTime endTime = searchTime.plusHours(durationHours);
    
    // Search for next 7 days
    for (int day = 0; day < 7; day++) {
        for (int hour = 8; hour < 22; hour++) { // 8 AM to 10 PM
            LocalDateTime slotStart = searchTime.toLocalDate().atTime(hour, 0).plusDays(day);
            LocalDateTime slotEnd = slotStart.plusHours(durationHours);
            
            if (slotStart.isAfter(fromTime) && !hasConflict(room, slotStart, slotEnd)) {
                NextAvailableSlotResponse response = new NextAvailableSlotResponse();
                response.setRoomId(roomId);
                response.setStartTime(slotStart);
                response.setEndTime(slotEnd);
                response.setDurationHours(durationHours);
                return response;
            }
        }
    }
    
    throw new IllegalStateException("No available slots found in the next 7 days");
}

// ========== BOOKED ROOMS AND AVAILABILITY METHODS ==========

public List<BookedRoomResponse> getCurrentlyBookedRooms() {
    LocalDateTime now = LocalDateTime.now();
    List<RoomBooking> activeBookings = roomBookingRepository.findCurrentlyActiveBookings(now);
    
    Map<Room, List<RoomBooking>> roomBookingsMap = activeBookings.stream()
            .collect(Collectors.groupingBy(RoomBooking::getRoom));
    
    return roomBookingsMap.entrySet().stream()
            .map(entry -> {
                Room room = entry.getKey();
                List<RoomBooking> bookings = entry.getValue();
                
                BookedRoomResponse response = new BookedRoomResponse();
                response.setRoom(mapRoomToResponse(room));
                response.setCurrentBooking(mapToResponse(bookings.get(0))); // Current booking
                response.setNextAvailableTime(findNextAvailableTime(room, now));
                response.setUpcomingBookingsCount(getUpcomingBookingsCount(room, now));
                
                return response;
            })
            .collect(Collectors.toList());
}

public RoomBookingTimelineResponse getRoomBookingTimeline(Long roomId, int days) {
    Room room = findRoomById(roomId);
    LocalDateTime startTime = LocalDateTime.now().toLocalDate().atStartOfDay();
    LocalDateTime endTime = startTime.plusDays(days);
    
    List<RoomBooking> bookings = roomBookingRepository.findBookingsInPeriod(room, startTime, endTime);
    
    RoomBookingTimelineResponse response = new RoomBookingTimelineResponse();
    response.setRoom(mapRoomToResponse(room));
    response.setStartDate(startTime);
    response.setEndDate(endTime);
    response.setTotalDays(days);
    
    // Group bookings by day
    Map<LocalDate, List<RoomBooking>> bookingsByDay = bookings.stream()
            .collect(Collectors.groupingBy(booking -> booking.getStartTime().toLocalDate()));
    
    List<DailyBookingTimelineResponse> dailyTimelines = new ArrayList<>();
    for (int i = 0; i < days; i++) {
        LocalDate date = startTime.toLocalDate().plusDays(i);
        List<RoomBooking> dayBookings = bookingsByDay.getOrDefault(date, new ArrayList<>());
        
        DailyBookingTimelineResponse dailyTimeline = new DailyBookingTimelineResponse();
        dailyTimeline.setDate(date);
        dailyTimeline.setBookings(dayBookings.stream()
                .map(this::mapToTimelineBooking)
                .collect(Collectors.toList()));
        dailyTimeline.setAvailableSlots(calculateAvailableSlots(date, dayBookings));
        dailyTimeline.setTotalBookedHours(calculateTotalBookedHours(dayBookings));
        dailyTimeline.setUtilizationPercentage(calculateUtilizationPercentage(dayBookings));
        
        dailyTimelines.add(dailyTimeline);
    }
    
    response.setDailyTimelines(dailyTimelines);
    response.setOverallUtilization(calculateOverallUtilization(bookings, days));
    
    return response;
}

public List<RoomNextAvailableResponse> getNextAvailableForBookedRooms(int durationHours) {
    LocalDateTime now = LocalDateTime.now();
    List<RoomBooking> activeBookings = roomBookingRepository.findCurrentlyActiveBookings(now);
    
    Set<Room> bookedRooms = activeBookings.stream()
            .map(RoomBooking::getRoom)
            .collect(Collectors.toSet());
    
    return bookedRooms.stream()
            .map(room -> {
                RoomNextAvailableResponse response = new RoomNextAvailableResponse();
                response.setRoom(mapRoomToResponse(room));
                response.setCurrentlyBooked(true);
                
                // Find current booking end time
                Optional<RoomBooking> currentBooking = activeBookings.stream()
                        .filter(booking -> booking.getRoom().equals(room))
                        .findFirst();
                
                if (currentBooking.isPresent()) {
                    response.setCurrentBookingEndTime(currentBooking.get().getEndTime());
                }
                
                // Find next available slot
                NextAvailableSlotResponse nextSlot = getNextAvailableSlot(room.getId(), durationHours, now);
                response.setNextAvailableTime(nextSlot.getStartTime());
                response.setDurationHours(durationHours);
                response.setHoursUntilAvailable(
                    Duration.between(now, nextSlot.getStartTime()).toHours()
                );
                
                return response;
            })
            .collect(Collectors.toList());
}

public List<AvailabilityGapResponse> getAvailabilityGaps(Long roomId, LocalDateTime startDate, 
                                                        LocalDateTime endDate, int minGapMinutes) {
    Room room = findRoomById(roomId);
    List<RoomBooking> bookings = roomBookingRepository.findBookingsInPeriod(room, startDate, endDate);
    
    // Sort bookings by start time
    bookings.sort(Comparator.comparing(RoomBooking::getStartTime));
    
    List<AvailabilityGapResponse> gaps = new ArrayList<>();
    LocalDateTime searchTime = startDate;
    
    for (RoomBooking booking : bookings) {
        if (searchTime.isBefore(booking.getStartTime())) {
            long gapMinutes = Duration.between(searchTime, booking.getStartTime()).toMinutes();
            
            if (gapMinutes >= minGapMinutes) {
                AvailabilityGapResponse gap = new AvailabilityGapResponse();
                gap.setRoomId(roomId);
                gap.setStartTime(searchTime);
                gap.setEndTime(booking.getStartTime());
                gap.setDurationMinutes((int) gapMinutes);
                gap.setDurationHours(gapMinutes / 60.0);
                gaps.add(gap);
            }
        }
        searchTime = booking.getEndTime().isAfter(searchTime) ? booking.getEndTime() : searchTime;
    }
    
    // Check for gap after last booking
    if (searchTime.isBefore(endDate)) {
        long gapMinutes = Duration.between(searchTime, endDate).toMinutes();
        if (gapMinutes >= minGapMinutes) {
            AvailabilityGapResponse gap = new AvailabilityGapResponse();
            gap.setRoomId(roomId);
            gap.setStartTime(searchTime);
            gap.setEndTime(endDate);
            gap.setDurationMinutes((int) gapMinutes);
            gap.setDurationHours(gapMinutes / 60.0);
            gaps.add(gap);
        }
    }
    
    return gaps;
}

public List<RoomOccupancyResponse> getRoomOccupancyStatus() {
    LocalDateTime now = LocalDateTime.now();
    List<Room> allRooms = roomRepository.findByAvailableAndRequiresBooking(true, true);
    
    return allRooms.stream()
            .map(room -> {
                RoomOccupancyResponse response = new RoomOccupancyResponse();
                response.setRoom(mapRoomToResponse(room));
                
                // Check current booking
                Optional<RoomBooking> currentBooking = roomBookingRepository
                        .findCurrentBookingForRoom(room, now);
                
                if (currentBooking.isPresent()) {
                    response.setOccupied(true);
                    response.setCurrentBooking(mapToResponse(currentBooking.get()));
                    response.setOccupiedUntil(currentBooking.get().getEndTime());
                    response.setOccupiedFor(Duration.between(now, currentBooking.get().getEndTime()).toMinutes());
                } else {
                    response.setOccupied(false);
                    response.setCurrentBooking(null);
                    
                    // Find next booking
                    Optional<RoomBooking> nextBooking = roomBookingRepository
                            .findNextBookingForRoom(room, now);
                    
                    if (nextBooking.isPresent()) {
                        response.setNextBookingTime(nextBooking.get().getStartTime());
                        response.setFreeFor(Duration.between(now, nextBooking.get().getStartTime()).toMinutes());
                    }
                }
                
                return response;
            })
            .collect(Collectors.toList());
}

public List<Object> getUpcomingBookings(Long roomId, int days, boolean includeDetails) {
    Room room = findRoomById(roomId);
    LocalDateTime now = LocalDateTime.now();
    LocalDateTime endTime = now.plusDays(days);
    
    List<RoomBooking> upcomingBookings = roomBookingRepository
            .findUpcomingBookingsForRoom(room, now, endTime);
    
    return upcomingBookings.stream()
            .map(booking -> {
                UpcomingBookingResponse response = new UpcomingBookingResponse();
                response.setId(booking.getId());
                response.setTitle(booking.getTitle());
                response.setStartTime(booking.getStartTime());
                response.setEndTime(booking.getEndTime());
                response.setStatus(booking.getStatus());
                response.setOrganizerName(booking.getUser().getFullName());
                response.setParticipantCount(booking.getParticipants().size() + 1);
                response.setHoursFromNow(Duration.between(now, booking.getStartTime()).toHours());
                
                if (includeDetails) {
                    response.setDescription(booking.getDescription());
                    response.setRequiresCheckIn(booking.isRequiresCheckIn());
                    response.setPublicBooking(booking.isPublic());
                }
                
                return response;
            })
            .collect(Collectors.toList());
}

public RoomAvailabilityCalendarResponse getAvailabilityCalendar(LocalDateTime startDate, int days, 
                                                              String category, String building) {
    LocalDateTime endDate = startDate.plusDays(days);
    
    // Filter rooms based on criteria
    List<Room> rooms = roomRepository.findByAvailableAndRequiresBooking(true, true);
    
    if (category != null) {
        try {
            RoomCategory roomCategory = RoomCategory.valueOf(category.toUpperCase());
            rooms = rooms.stream()
                    .filter(room -> room.getCategory() == roomCategory)
                    .collect(Collectors.toList());
        } catch (IllegalArgumentException e) {
            rooms = new ArrayList<>();
        }
    }
    
    if (building != null) {
        rooms = rooms.stream()
                .filter(room -> room.getBuilding().equalsIgnoreCase(building))
                .collect(Collectors.toList());
    }
    
    RoomAvailabilityCalendarResponse response = new RoomAvailabilityCalendarResponse();
    response.setStartDate(startDate);
    response.setEndDate(endDate);
    response.setTotalDays(days);
    
    List<RoomCalendarResponse> roomCalendars = rooms.stream()
            .map(room -> {
                RoomCalendarResponse roomCalendar = new RoomCalendarResponse();
                roomCalendar.setRoom(mapRoomToResponse(room));
                
                List<RoomBooking> roomBookings = roomBookingRepository
                        .findBookingsInPeriod(room, startDate, endDate);
                
                roomCalendar.setBookings(roomBookings.stream()
                        .map(this::mapToCalendarBooking)
                        .collect(Collectors.toList()));
                
                roomCalendar.setUtilizationPercentage(
                        calculateUtilizationPercentage(roomBookings, days));
                
                return roomCalendar;
            })
            .collect(Collectors.toList());
    
    response.setRoomCalendars(roomCalendars);
    response.setTotalRooms(rooms.size());
    response.setOverallUtilization(calculateOverallCalendarUtilization(roomCalendars));
    
    return response;
}

public CurrentBookingResponse getCurrentBooking(Long roomId) {
    Room room = findRoomById(roomId);
    LocalDateTime now = LocalDateTime.now();
    
    Optional<RoomBooking> currentBooking = roomBookingRepository
            .findCurrentBookingForRoom(room, now);
    
    CurrentBookingResponse response = new CurrentBookingResponse();
    response.setRoomId(roomId);
    response.setCurrentTime(now);
    
    if (currentBooking.isPresent()) {
        RoomBooking booking = currentBooking.get();
        response.setHasCurrentBooking(true);
        response.setBooking(mapToResponse(booking));
        response.setTimeRemaining(Duration.between(now, booking.getEndTime()).toMinutes());
        response.setCanCheckIn(booking.canCheckIn());
        response.setCheckedIn(booking.getCheckedInAt() != null);
    } else {
        response.setHasCurrentBooking(false);
        
        // Find next booking
        Optional<RoomBooking> nextBooking = roomBookingRepository
                .findNextBookingForRoom(room, now);
        
        if (nextBooking.isPresent()) {
            response.setNextBookingTime(nextBooking.get().getStartTime());
            response.setTimeUntilNext(Duration.between(now, nextBooking.get().getStartTime()).toMinutes());
        }
    }
    
    return response;
}

public RoomUtilizationDashboardResponse getRoomUtilizationDashboard(int days) {
    LocalDateTime startTime = LocalDateTime.now().minusDays(days).toLocalDate().atStartOfDay();
    LocalDateTime endTime = LocalDateTime.now();
    
    List<Room> allRooms = roomRepository.findByAvailableAndRequiresBooking(true, true);
    List<RoomBooking> allBookings = roomBookingRepository.findBookingsInPeriod(startTime, endTime);
    
    RoomUtilizationDashboardResponse response = new RoomUtilizationDashboardResponse();
    response.setPeriodStart(startTime);
    response.setPeriodEnd(endTime);
    response.setTotalDays(days);
    response.setTotalRooms(allRooms.size());
    response.setTotalBookings(allBookings.size());
    
    // Current status
    LocalDateTime now = LocalDateTime.now();
    long currentlyOccupied = allRooms.stream()
            .mapToLong(room -> roomBookingRepository.findCurrentBookingForRoom(room, now).isPresent() ? 1 : 0)
            .sum();
    
    response.setCurrentlyOccupiedRooms((int) currentlyOccupied);
    response.setCurrentOccupancyPercentage((currentlyOccupied * 100.0) / allRooms.size());
    
    // Calculate metrics
    response.setAverageBookingDuration(calculateAverageBookingDuration(allBookings));
    response.setMostPopularRoom(findMostPopularRoom(allBookings));
    response.setLeastUsedRoom(findLeastUsedRoom(allRooms, allBookings));
    response.setPeakHours(findPeakHours(allBookings));
    
    // Room utilization breakdown
    List<RoomUtilizationSummary> roomSummaries = allRooms.stream()
            .map(room -> {
                List<RoomBooking> roomBookings = allBookings.stream()
                        .filter(booking -> booking.getRoom().equals(room))
                        .collect(Collectors.toList());
                
                RoomUtilizationSummary summary = new RoomUtilizationSummary();
                summary.setRoom(mapRoomToResponse(room));
                summary.setTotalBookings(roomBookings.size());
                summary.setTotalHours(calculateTotalHours(roomBookings));
                summary.setUtilizationPercentage(calculateUtilizationPercentage(roomBookings, days));
                summary.setAverageBookingDuration(calculateAverageBookingDuration(roomBookings));
                
                return summary;
            })
            .collect(Collectors.toList());
    
    response.setRoomUtilizations(roomSummaries);
    response.setOverallUtilization(calculateOverallUtilization(allBookings, days));
    
    return response;
}

// ========== HELPER METHODS ==========

private LocalDateTime findNextAvailableTime(Room room, LocalDateTime from) {
    // Find the next available slot after current bookings
    try {
        NextAvailableSlotResponse nextSlot = getNextAvailableSlot(room.getId(), 1, from);
        return nextSlot.getStartTime();
    } catch (Exception e) {
        return from.plusDays(1); // Default to next day if no slots found
    }
}

private int getUpcomingBookingsCount(Room room, LocalDateTime from) {
    LocalDateTime endOfDay = from.toLocalDate().atTime(23, 59);
    List<RoomBooking> upcoming = roomBookingRepository.findUpcomingBookingsForRoom(room, from, endOfDay);
    return upcoming.size();
}

private TimelineBookingResponse mapToTimelineBooking(RoomBooking booking) {
    TimelineBookingResponse response = new TimelineBookingResponse();
    response.setId(booking.getId());
    response.setTitle(booking.getTitle());
    response.setStartTime(booking.getStartTime());
    response.setEndTime(booking.getEndTime());
    response.setStatus(booking.getStatus());
    response.setOrganizerName(booking.getUser().getFullName());
    response.setParticipantCount(booking.getParticipants().size() + 1);
    response.setPublicBooking(booking.isPublic());
    return response;
}

private List<AvailableSlotResponse> calculateAvailableSlots(LocalDate date, List<RoomBooking> bookings) {
    List<AvailableSlotResponse> slots = new ArrayList<>();
    LocalDateTime dayStart = date.atTime(8, 0); // 8 AM
    LocalDateTime dayEnd = date.atTime(22, 0);  // 10 PM
    
    bookings.sort(Comparator.comparing(RoomBooking::getStartTime));
    
    LocalDateTime currentTime = dayStart;
    for (RoomBooking booking : bookings) {
        if (currentTime.isBefore(booking.getStartTime())) {
            AvailableSlotResponse slot = new AvailableSlotResponse();
            slot.setStartTime(currentTime);
            slot.setEndTime(booking.getStartTime());
            slot.setDurationMinutes(Duration.between(currentTime, booking.getStartTime()).toMinutes());
            slots.add(slot);
        }
        currentTime = booking.getEndTime().isAfter(currentTime) ? booking.getEndTime() : currentTime;
    }
    
    // Add final slot if there's time left in the day
    if (currentTime.isBefore(dayEnd)) {
        AvailableSlotResponse slot = new AvailableSlotResponse();
        slot.setStartTime(currentTime);
        slot.setEndTime(dayEnd);
        slot.setDurationMinutes(Duration.between(currentTime, dayEnd).toMinutes());
        slots.add(slot);
    }
    
    return slots;
}

private double calculateTotalBookedHours(List<RoomBooking> bookings) {
    return bookings.stream()
            .mapToDouble(booking -> Duration.between(booking.getStartTime(), booking.getEndTime()).toMinutes() / 60.0)
            .sum();
}

private double calculateUtilizationPercentage(List<RoomBooking> bookings) {
    if (bookings.isEmpty()) return 0.0;
    
    double totalBookedHours = calculateTotalBookedHours(bookings);
    double totalAvailableHours = 14.0; // 8 AM to 10 PM = 14 hours per day
    
    return (totalBookedHours / totalAvailableHours) * 100.0;
}

private double calculateUtilizationPercentage(List<RoomBooking> bookings, int days) {
    if (bookings.isEmpty()) return 0.0;
    
    double totalBookedHours = calculateTotalHours(bookings);
    double totalAvailableHours = 14.0 * days; // 14 hours per day
    
    return (totalBookedHours / totalAvailableHours) * 100.0;
}

private double calculateOverallUtilization(List<RoomBooking> bookings, int days) {
    return calculateUtilizationPercentage(bookings, days);
}

private CalendarBookingResponse mapToCalendarBooking(RoomBooking booking) {
    CalendarBookingResponse response = new CalendarBookingResponse();
    response.setId(booking.getId());
    response.setTitle(booking.getTitle());
    response.setStartTime(booking.getStartTime());
    response.setEndTime(booking.getEndTime());
    response.setStatus(booking.getStatus());
    response.setOrganizerName(booking.getUser().getFullName());
    response.setPublicBooking(booking.isPublic());
    return response;
}

private double calculateOverallCalendarUtilization(List<RoomCalendarResponse> roomCalendars) {
    return roomCalendars.stream()
            .mapToDouble(RoomCalendarResponse::getUtilizationPercentage)
            .average()
            .orElse(0.0);
}

private double calculateAverageBookingDuration(List<RoomBooking> bookings) {
    if (bookings.isEmpty()) return 0.0;
    
    return bookings.stream()
            .mapToDouble(booking -> Duration.between(booking.getStartTime(), booking.getEndTime()).toHours())
            .average()
            .orElse(0.0);
}

private RoomResponse findMostPopularRoom(List<RoomBooking> bookings) {
    Map<Room, Long> roomCounts = bookings.stream()
            .collect(Collectors.groupingBy(RoomBooking::getRoom, Collectors.counting()));
    
    return roomCounts.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(entry -> mapRoomToResponse(entry.getKey()))
            .orElse(null);
}

private RoomResponse findLeastUsedRoom(List<Room> allRooms, List<RoomBooking> bookings) {
    Map<Room, Long> roomCounts = bookings.stream()
            .collect(Collectors.groupingBy(RoomBooking::getRoom, Collectors.counting()));
    
    return allRooms.stream()
            .min(Comparator.comparing(room -> roomCounts.getOrDefault(room, 0L)))
            .map(this::mapRoomToResponse)
            .orElse(null);
}

private List<Integer> findPeakHours(List<RoomBooking> bookings) {
    Map<Integer, Long> hourCounts = bookings.stream()
            .collect(Collectors.groupingBy(
                    booking -> booking.getStartTime().getHour(),
                    Collectors.counting()
            ));
    
    // Find the maximum count
    Long maxCount = hourCounts.values().stream()
            .max(Long::compareTo)
            .orElse(0L);
    
    // Return all hours with the maximum count
    return hourCounts.entrySet().stream()
            .filter(entry -> entry.getValue().equals(maxCount))
            .map(Map.Entry::getKey)
            .sorted()
            .collect(Collectors.toList());
}

private double calculateTotalHours(List<RoomBooking> bookings) {
    return bookings.stream()
            .mapToDouble(booking -> Duration.between(booking.getStartTime(), booking.getEndTime()).toHours())
            .sum();
}

// @Transactional
// public MessageResponse updateRoomBookingEquipmentRequests(Long bookingId, List<Long> equipmentIds, String userEmail) {
//     RoomBooking booking = findBookingById(bookingId);
//     User user = findUserByEmail(userEmail);
    
//     // Validate user owns this booking
//     if (!booking.getUser().equals(user)) {
//         throw new IllegalArgumentException("You can only modify your own bookings");
//     }
    
//     // Clear existing equipment requests
//     booking.getRequestedEquipment().clear();
    
//     // Add new equipment requests
//     if (equipmentIds != null && !equipmentIds.isEmpty()) {
//         Set<Equipment> equipment = equipmentIds.stream()
//                 .map(this::findEquipmentById)
//                 .filter(eq -> {
//                     // For students, only allow equipment marked as allowedToStudents
//                     if (hasRole(user, "ROLE_USER")) {
//                         return eq.isAllowedToStudents();
//                     }
//                     return true; // Professors can request any equipment
//                 })
//                 .collect(Collectors.toSet());
//         booking.setRequestedEquipment(equipment);
//     }
    
//     roomBookingRepository.save(booking);
//     return new MessageResponse("Equipment requests updated successfully");
// }

}
