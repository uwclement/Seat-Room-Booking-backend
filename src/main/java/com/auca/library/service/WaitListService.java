package com.auca.library.service;

import com.auca.library.dto.request.WaitListRequest;
import com.auca.library.dto.response.WaitListDTO;
import com.auca.library.exception.BadRequestException;
import com.auca.library.exception.ResourceNotFoundException;
import com.auca.library.model.Seat;
import com.auca.library.model.User;
import com.auca.library.model.WaitList;
import com.auca.library.repository.SeatRepository;
import com.auca.library.repository.UserRepository;
import com.auca.library.repository.WaitListRepository;
import org.springframework.beans.factory.annotation.Autowired;
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

   @Autowired
   private WaitListRepository waitListRepository;
   
   @Autowired
   private SeatRepository seatRepository;
   
   @Autowired
   private UserRepository userRepository;
   
   @Autowired
   private SeatService seatService;
   
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
}