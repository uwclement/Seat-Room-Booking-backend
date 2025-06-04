package com.auca.library.service;

import com.auca.library.dto.response.RoomResponse;
import com.auca.library.dto.response.SmartRoomSuggestionResponse;
import com.auca.library.exception.ResourceNotFoundException;
import com.auca.library.model.*;
import com.auca.library.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class SmartRoomSuggestionService {
    
    @Autowired private RoomRepository roomRepository;
    @Autowired private RoomBookingRepository roomBookingRepository;
    @Autowired private UserFavoriteRoomRepository favoriteRoomRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private RoomAvailabilityService roomAvailabilityService;

    
    public SmartRoomSuggestionResponse getSmartSuggestions(String userEmail, LocalDateTime desiredStart, 
                                                          LocalDateTime desiredEnd, Set<Long> requiredEquipmentIds) {
        User user = findUserByEmail(userEmail);
        List<Room> allRooms = roomRepository.findByAvailableTrue();
        
        SmartRoomSuggestionResponse response = new SmartRoomSuggestionResponse();
        List<SmartRoomSuggestionResponse.RoomSuggestion> suggestions = new ArrayList<>();
        
        // Score and rank rooms
        for (Room room : allRooms) {
            SmartRoomSuggestionResponse.RoomSuggestion suggestion = createRoomSuggestion(
                    room, user, desiredStart, desiredEnd, requiredEquipmentIds);
            if (suggestion.getMatchScore() > 0.1) { // Only include relevant suggestions
                suggestions.add(suggestion);
            }
        }
        
        // Sort by match score
        suggestions.sort((a, b) -> Double.compare(b.getMatchScore(), a.getMatchScore()));
        
        response.setSuggestions(suggestions.stream().limit(10).collect(Collectors.toList()));
        response.setBasedOn(determineSuggestionBasis(user, requiredEquipmentIds));
        
        return response;
    }
    
    private SmartRoomSuggestionResponse.RoomSuggestion createRoomSuggestion(Room room, User user, 
                                                                           LocalDateTime desiredStart, 
                                                                           LocalDateTime desiredEnd, 
                                                                           Set<Long> requiredEquipmentIds) {
        SmartRoomSuggestionResponse.RoomSuggestion suggestion = new SmartRoomSuggestionResponse.RoomSuggestion();
        suggestion.setRoom(mapRoomToResponse(room));
        
        double score = 0.0;
        List<String> reasons = new ArrayList<>();
        
        // 1. Historical usage score (30%)
        double historyScore = calculateHistoryScore(room, user);
        score += historyScore * 0.3;
        if (historyScore > 0.5) {
            reasons.add("You've used this room frequently");
        }
        
        // 2. Favorite rooms score (20%)
        if (favoriteRoomRepository.existsByUserAndRoom(user, room)) {
            score += 0.2;
            reasons.add("This is one of your favorite rooms");
        }
        
        // 3. Equipment match score (25%)
        double equipmentScore = calculateEquipmentScore(room, requiredEquipmentIds);
        score += equipmentScore * 0.25;
        if (equipmentScore > 0.8) {
            reasons.add("Has all requested equipment");
        } else if (equipmentScore > 0.5) {
            reasons.add("Has most requested equipment");
        }
        
        // 4. Availability score (25%)
        double availabilityScore = calculateAvailabilityScore(room, desiredStart, desiredEnd);
        score += availabilityScore * 0.25;
        suggestion.setAvailable(availabilityScore > 0.9);
        
        if (availabilityScore == 1.0) {
            reasons.add("Available at requested time");
        } else if (availabilityScore > 0.5) {
            reasons.add("Available with minor adjustments");
            // Generate alternative time suggestions
            suggestion.setSuggestedTimes(generateAlternativeTimes(room, desiredStart, desiredEnd));
        } else {
            // Find next available time
            Optional<LocalDateTime> nextAvailable = roomBookingRepository.findNextAvailableTime(room, desiredStart);
            if (nextAvailable.isPresent()) {
                suggestion.setNextAvailableTime(nextAvailable.get());
                reasons.add("Next available: " + nextAvailable.get().toString());
            }
        }
        
        suggestion.setMatchScore(Math.min(1.0, score));
        suggestion.setReasons(reasons);
        
        return suggestion;
    }
    
    private double calculateHistoryScore(Room room, User user) {
        // Get user's booking history for this room
        List<RoomBooking> userBookings = roomBookingRepository.findUserBookingHistory(user);
        long roomBookings = userBookings.stream()
                .filter(booking -> booking.getRoom().equals(room))
                .count();
        
        if (userBookings.isEmpty()) return 0.0;
        
        return Math.min(1.0, (double) roomBookings / userBookings.size() * 2);
    }
    
    private double calculateEquipmentScore(Room room, Set<Long> requiredEquipmentIds) {
        if (requiredEquipmentIds == null || requiredEquipmentIds.isEmpty()) {
            return 0.5; // Neutral score when no equipment required
        }
        
        Set<Long> roomEquipmentIds = room.getEquipment().stream()
                .map(Equipment::getId)
                .collect(Collectors.toSet());
        
        long matchingEquipment = requiredEquipmentIds.stream()
                .filter(roomEquipmentIds::contains)
                .count();
        
        return (double) matchingEquipment / requiredEquipmentIds.size();
    }
    
    private double calculateAvailabilityScore(Room room, LocalDateTime desiredStart, LocalDateTime desiredEnd) {
        // Check if room is available at exact time
        if (roomBookingRepository.countConflictingBookings(room, desiredStart, desiredEnd) == 0) {
            return 1.0;
        }
        
        // Check availability within 2 hours before/after
        LocalDateTime flexStart = desiredStart.minusHours(2);
        LocalDateTime flexEnd = desiredEnd.plusHours(2);
        
        List<RoomBooking> bookings = roomBookingRepository.findActiveBookingsForRoom(room, flexStart, flexEnd);
        
        // Calculate percentage of requested time that's available
        long totalMinutes = ChronoUnit.MINUTES.between(desiredStart, desiredEnd);
        long conflictMinutes = 0;
        
        for (RoomBooking booking : bookings) {
            LocalDateTime overlapStart = booking.getStartTime().isAfter(desiredStart) ? booking.getStartTime() : desiredStart;
            LocalDateTime overlapEnd = booking.getEndTime().isBefore(desiredEnd) ? booking.getEndTime() : desiredEnd;
            
            if (overlapStart.isBefore(overlapEnd)) {
                conflictMinutes += ChronoUnit.MINUTES.between(overlapStart, overlapEnd);
            }
        }
        
        return Math.max(0.0, 1.0 - (double) conflictMinutes / totalMinutes);
    }
    
    private List<SmartRoomSuggestionResponse.RoomSuggestion.TimeSlot> generateAlternativeTimes(Room room, 
                                                                                              LocalDateTime desiredStart, 
                                                                                              LocalDateTime desiredEnd) {
        List<SmartRoomSuggestionResponse.RoomSuggestion.TimeSlot> alternatives = new ArrayList<>();
        long durationMinutes = ChronoUnit.MINUTES.between(desiredStart, desiredEnd);
        
        // Check 2 hours before and after
        for (int offset = -120; offset <= 120; offset += 30) {
            LocalDateTime altStart = desiredStart.plusMinutes(offset);
            LocalDateTime altEnd = altStart.plusMinutes(durationMinutes);
            
            if (roomBookingRepository.countConflictingBookings(room, altStart, altEnd) == 0) {
                SmartRoomSuggestionResponse.RoomSuggestion.TimeSlot slot = 
                        new SmartRoomSuggestionResponse.RoomSuggestion.TimeSlot();
                slot.setStartTime(altStart);
                slot.setEndTime(altEnd);
                slot.setReason(offset == 0 ? "Exact match" : 
                              (offset < 0 ? "Earlier option" : "Later option"));
                alternatives.add(slot);
                
                if (alternatives.size() >= 3) break; // Limit alternatives
            }
        }
        
        return alternatives;
    }
    
    private String determineSuggestionBasis(User user, Set<Long> requiredEquipmentIds) {
        if (requiredEquipmentIds != null && !requiredEquipmentIds.isEmpty()) {
            return "equipment";
        }
        
        if (favoriteRoomRepository.findByUserOrderByCreatedAtDesc(user).size() > 0) {
            return "preferences";
        }
        
        List<RoomBooking> history = roomBookingRepository.findUserBookingHistory(user);
        if (history.size() > 3) {
            return "history";
        }
        
        return "availability";
    }
    
    private User findUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + email));
    }
    
    private RoomResponse mapRoomToResponse(Room room) {
        // Map Room entity to RoomResponse DTO
        RoomResponse response = new RoomResponse();
        // ... mapping logic
        return response;
    }
}