package com.auca.library.service;

import com.auca.library.dto.request.WaitListRequest;
import com.auca.library.dto.response.WaitListDTO;
import com.auca.library.dto.response.WaitlistResponse;
import com.auca.library.exception.BadRequestException;
import com.auca.library.exception.ResourceNotFoundException;
import com.auca.library.model.Room;
import com.auca.library.model.RoomBooking;
import com.auca.library.model.RoomWaitlist;
import com.auca.library.model.Seat;
import com.auca.library.model.User;
import com.auca.library.model.WaitList;
import com.auca.library.repository.RoomBookingRepository;
import com.auca.library.repository.RoomRepository;
import com.auca.library.repository.RoomWaitlistRepository;
import com.auca.library.repository.SeatRepository;
import com.auca.library.repository.UserRepository;
import com.auca.library.repository.WaitListRepository;

import jakarta.mail.MessagingException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class WaitListService {
   
   @Autowired private BookingService bookingService;

   @Autowired private EmailService emailService;
    
   @Autowired private NotificationService notificationService;

   @Autowired private WaitListRepository waitListRepository;
   
   @Autowired private SeatRepository seatRepository;
   
   @Autowired private UserRepository userRepository;
   
   @Autowired private SeatService seatService;
   
   @Autowired private RoomWaitlistRepository waitlistRepository;

   @Autowired private RoomBookingRepository roomBookingRepository;

   @Autowired private RoomRepository roomRepository;

   @Transactional
   public WaitListDTO joinWaitList(WaitListRequest request) {
       // Get current user
       Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
       String currentUserEmail = authentication.getName();
       
       User user = userRepository.findByEmail(currentUserEmail)
               .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + currentUserEmail));
       
       Seat seat = seatRepository.findById(request.getSeatId())
               .orElseThrow(() -> new ResourceNotFoundException("Seat not found with id: " + request.getSeatId()));
       
       // Check if user is already on the wait list for this seat
       List<WaitList> existingRequests = waitListRepository.findActiveWaitListItemByUserAndSeat(user.getId(), seat.getId());
       
       if (!existingRequests.isEmpty()) {
           throw new BadRequestException("You are already on the wait list for this seat");
       }
       
       // Validate request times
       validateWaitListRequest(request.getRequestedStartTime(), request.getRequestedEndTime());
       
       // Create new wait list entry
       WaitList waitList = new WaitList(user, seat, request.getRequestedStartTime(), request.getRequestedEndTime());
       
       // Calculate queue position
       int queuePosition = waitListRepository.countWaitingForSeat(seat.getId()) + 1;
       waitList.setQueuePosition(queuePosition);
       
       // Save to database
       waitList = waitListRepository.save(waitList);
       
       return mapWaitListToDTO(waitList);
   }
   
   @Transactional
   public WaitListDTO cancelWaitList(Long id) {
       Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
       String currentUserEmail = authentication.getName();
       
       User user = userRepository.findByEmail(currentUserEmail)
               .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + currentUserEmail));
       
       WaitList waitList = waitListRepository.findById(id)
               .orElseThrow(() -> new ResourceNotFoundException("Wait list entry not found with id: " + id));
       
       // Make sure the wait list entry belongs to the current user
       if (!waitList.getUser().getId().equals(user.getId())) {
           throw new BadRequestException("You don't have permission to cancel this wait list entry");
       }
       
       // Update status
       waitList.setStatus(WaitList.WaitListStatus.CANCELLED);
       waitList = waitListRepository.save(waitList);
       
       // Reorder queue positions for remaining wait list entries
       reorderWaitList(waitList.getSeat().getId());
       
       return mapWaitListToDTO(waitList);
   }
   
   public List<WaitListDTO> getUserWaitList() {
       Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
       String currentUserEmail = authentication.getName();
       
       User user = userRepository.findByEmail(currentUserEmail)
               .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + currentUserEmail));
       
       List<WaitList> waitList = waitListRepository.findByUserAndStatusOrderByCreatedAtDesc(
               user, WaitList.WaitListStatus.WAITING);
       
       return waitList.stream()
               .map(this::mapWaitListToDTO)
               .collect(Collectors.toList());
   }
   
   private void validateWaitListRequest(LocalDateTime startTime, LocalDateTime endTime) {
       LocalDateTime now = LocalDateTime.now();
       
       // Check if start time is in the future
       if (startTime.isBefore(now)) {
           throw new BadRequestException("Start time must be in the future");
       }
       
       // Check if end time is after start time
       if (endTime.isBefore(startTime)) {
           throw new BadRequestException("End time must be after start time");
       }
       
       // Check maximum duration (6 hours)
       Duration duration = Duration.between(startTime, endTime);
       if (duration.toHours() > 6) {
           throw new BadRequestException("Wait list request duration cannot exceed 6 hours");
       }
   }
   
   private void reorderWaitList(Long seatId) {
       List<WaitList> waitingList = waitListRepository.findWaitingListForSeat(seatId);
       
       // Update queue positions
       for (int i = 0; i < waitingList.size(); i++) {
           WaitList item = waitingList.get(i);
           item.setQueuePosition(i + 1);
           waitListRepository.save(item);
       }
   }
   
   private WaitListDTO mapWaitListToDTO(WaitList waitList) {
       WaitListDTO dto = new WaitListDTO();
       
       dto.setId(waitList.getId());
       dto.setUserId(waitList.getUser().getId());
       dto.setUserName(waitList.getUser().getFullName());
       dto.setSeatId(waitList.getSeat().getId());
       dto.setSeatNumber(waitList.getSeat().getSeatNumber());
       dto.setRequestedStartTime(waitList.getRequestedStartTime());
       dto.setRequestedEndTime(waitList.getRequestedEndTime());
       dto.setCreatedAt(waitList.getCreatedAt());
       dto.setQueuePosition(waitList.getQueuePosition());
       dto.setNotified(waitList.isNotified());
       dto.setNotifiedAt(waitList.getNotifiedAt());
       dto.setStatus(waitList.getStatus());
       
       return dto;
   }


//    private void notifyWaitListUsers(Long seatId, LocalDateTime startTime, LocalDateTime endTime) {
//     // Find users waiting for this seat with overlapping time
//     List<WaitList> waitingList = waitListRepository.findWaitingListForSeat(seatId);
    
//     for (WaitList waitItem : waitingList) {
//         if (bookingService.isTimeOverlapping(waitItem.getRequestedStartTime(), waitItem.getRequestedEndTime(), 
//                            startTime, endTime) && !waitItem.isNotified()) {
            
//             // Update wait list item
//             waitItem.setNotified(true);
//             waitItem.setNotifiedAt(LocalDateTime.now());
//             waitItem.setStatus(WaitList.WaitListStatus.NOTIFIED);
//             waitListRepository.save(waitItem);
            
//             Seat seat = waitItem.getSeat();
            
//             // Send in-app notification
//             notificationService.sendWaitListNotification(
//                 waitItem.getUser(),
//                 seat,
//                 waitItem.getRequestedStartTime(),
//                 waitItem.getRequestedEndTime()
//             );
            
//             // Send email notification
//             try {
//                 emailService.sendWaitListNotification(
//                     waitItem.getUser().getEmail(),
//                     seat.getSeatNumber(),
//                     waitItem.getRequestedStartTime(),
//                     waitItem.getRequestedEndTime());
//             } catch (MessagingException e) {
//                 // Log error but continue processing
//                 System.err.println("Failed to send wait list notification: " + e.getMessage());
//             }
//         }
//     }
// }

 @Transactional
    public WaitlistResponse addToWaitlist(String userEmail, Long roomId, 
                                         LocalDateTime desiredStartTime, LocalDateTime desiredEndTime) {
        User user = findUserByEmail(userEmail);
        Room room = findRoomById(roomId);
        
        // Check if user already on waitlist for this time slot
        List<RoomWaitlist> existingWaitlist = waitlistRepository.findWaitlistForTimeSlot(
            room, desiredStartTime, desiredEndTime);
        
        boolean userAlreadyWaiting = existingWaitlist.stream()
            .anyMatch(w -> w.getUser().equals(user) && w.isActive());
        
        if (userAlreadyWaiting) {
            throw new IllegalStateException("User is already on waitlist for this time slot");
        }
        
        RoomWaitlist waitlistEntry = new RoomWaitlist();
        waitlistEntry.setUser(user);
        waitlistEntry.setRoom(room);
        waitlistEntry.setDesiredStartTime(desiredStartTime);
        waitlistEntry.setDesiredEndTime(desiredEndTime);
        waitlistEntry.setPriority(calculatePriority(user, room));
        waitlistEntry.setExpiresAt(desiredStartTime.minusHours(1)); // Expire 1 hour before desired time
        
        waitlistEntry = waitlistRepository.save(waitlistEntry);
        
        WaitlistResponse response = mapToResponse(waitlistEntry);
        response.setPositionInQueue(getPositionInQueue(waitlistEntry));
        
        return response;
    }
    
    @Transactional
    public void processWaitlistForAvailableSlot(Room room, LocalDateTime startTime, LocalDateTime endTime) {
        List<RoomWaitlist> waitingUsers = waitlistRepository.findWaitlistForTimeSlot(room, startTime, endTime);
        
        for (RoomWaitlist waitlistEntry : waitingUsers) {
            if (waitlistEntry.isActive() && !waitlistEntry.isNotificationSent()) {
                // Notify user about availability
                notificationService.addNotification(
                    waitlistEntry.getUser().getEmail(),
                    "Room Available from Waitlist",
                    String.format("The room %s is now available for your requested time: %s - %s. You have 15 minutes to book it.",
                        room.getName(), startTime, endTime),
                    "WAITLIST_AVAILABLE"
                );
                
                waitlistEntry.setNotificationSent(true);
                waitlistRepository.save(waitlistEntry);
                
                // Only notify the first user (highest priority)
                break;
            }
        }
    }
    
    @Transactional
    public void removeFromWaitlist(Long waitlistId, String userEmail) {
        RoomWaitlist waitlistEntry = waitlistRepository.findById(waitlistId)
            .orElseThrow(() -> new ResourceNotFoundException("Waitlist entry not found"));
        
        if (!waitlistEntry.getUser().getEmail().equals(userEmail)) {
            throw new SecurityException("User cannot remove this waitlist entry");
        }
        
        waitlistEntry.setActive(false);
        waitlistRepository.save(waitlistEntry);
    }
    
    public List<WaitlistResponse> getUserWaitlist(String userEmail) {
        User user = findUserByEmail(userEmail);
        List<RoomWaitlist> waitlistEntries = waitlistRepository.findByUserAndIsActiveTrueOrderByCreatedAtDesc(user);
        
        return waitlistEntries.stream()
            .map(entry -> {
                WaitlistResponse response = mapToResponse(entry);
                response.setPositionInQueue(getPositionInQueue(entry));
                response.setEstimatedAvailabilityTime(estimateAvailabilityTime(entry));
                return response;
            })
            .collect(Collectors.toList());
    }
    
    @Scheduled(fixedRate = 300000) // Every 5 minutes
    @Transactional
    public void cleanupExpiredWaitlistEntries() {
        LocalDateTime now = LocalDateTime.now();
        List<RoomWaitlist> expiredEntries = waitlistRepository.findExpiredWaitlistEntries(now);
        
        for (RoomWaitlist entry : expiredEntries) {
            entry.setActive(false);
            waitlistRepository.save(entry);
        }
    }
    
    private Integer calculatePriority(User user, Room room) {
        // Higher priority for users with booking history in this room
        List<RoomBooking> userBookingHistory = roomBookingRepository.findUserBookingHistory(user);
        long roomBookings = userBookingHistory.stream()
            .filter(booking -> booking.getRoom().equals(room))
            .count();
        
        return (int) Math.min(10, roomBookings); // Max priority of 10
    }
    
    private Integer getPositionInQueue(RoomWaitlist waitlistEntry) {
        List<RoomWaitlist> queuedUsers = waitlistRepository.findWaitlistForTimeSlot(
            waitlistEntry.getRoom(), 
            waitlistEntry.getDesiredStartTime(), 
            waitlistEntry.getDesiredEndTime()
        );
        
        return queuedUsers.indexOf(waitlistEntry) + 1;
    }
    
    private LocalDateTime estimateAvailabilityTime(RoomWaitlist waitlistEntry) {
        // Simple estimation based on current bookings
        List<RoomBooking> activeBookings = roomBookingRepository.findActiveBookingsForRoom(
            waitlistEntry.getRoom(), 
            LocalDateTime.now(), 
            waitlistEntry.getDesiredEndTime()
        );
        
        if (activeBookings.isEmpty()) {
            return LocalDateTime.now();
        }
        
        return activeBookings.stream()
            .map(RoomBooking::getEndTime)
            .min(LocalDateTime::compareTo)
            .orElse(waitlistEntry.getDesiredStartTime());
    }
    
    private WaitlistResponse mapToResponse(RoomWaitlist waitlistEntry) {
        WaitlistResponse response = new WaitlistResponse();
        response.setId(waitlistEntry.getId());
        // Map other fields...
        return response;
    }
    
    private User findUserByEmail(String email) {
        return userRepository.findByEmail(email)
            .orElseThrow(() -> new ResourceNotFoundException("User not found: " + email));
    }
    
    private Room findRoomById(Long roomId) {
        return roomRepository.findById(roomId)
            .orElseThrow(() -> new ResourceNotFoundException("Room not found: " + roomId));
    }


}