package com.auca.library.service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.auca.library.dto.response.RoomAvailabilityResponse;
import com.auca.library.dto.response.RoomResponse;
import com.auca.library.dto.response.WeeklyRoomAvailabilityResponse;
import com.auca.library.exception.ResourceNotFoundException;
import com.auca.library.model.Location;
import com.auca.library.model.Room;
import com.auca.library.model.RoomBooking;
import com.auca.library.repository.BookingParticipantRepository;
import com.auca.library.repository.RoomBookingRepository;
import com.auca.library.repository.RoomRepository;

@Service
public class RoomAvailabilityService {
    
    @Autowired private RoomBookingRepository roomBookingRepository;
    @Autowired private RoomRepository roomRepository;
    @Autowired private LibraryScheduleService scheduleService; // Assuming you have this
    @Autowired private BookingParticipantRepository participantRepository;

    
    // Get real-time room availability
    public RoomAvailabilityResponse getRoomAvailability(Long roomId) {
        Room room = findRoomById(roomId);
        LocalDateTime now = LocalDateTime.now();
        
        RoomAvailabilityResponse response = new RoomAvailabilityResponse();
        response.setRoom(mapRoomToResponse(room));
        
        // Check current availability
        List<RoomBooking> currentBookings = roomBookingRepository.findActiveBookingsForRoom(
                room, now, now.plusHours(1)
        );
        
        response.setCurrentlyAvailable(currentBookings.isEmpty());
        
        if (!currentBookings.isEmpty()) {
            RoomBooking currentBooking = currentBookings.get(0);
            response.setCurrentBookingEndTime(currentBooking.getEndTime());
        }
        
        // Find next available time
        Optional<LocalDateTime> nextAvailable = roomBookingRepository.findNextAvailableTime(room, now);
        response.setNextAvailableTime(nextAvailable.orElse(now));
        
        // Generate available slots for today and tomorrow
        response.setAvailableSlots(generateAvailableSlots(room, now, now.plusDays(2)));
        response.setBookedSlots(generateBookedSlots(room, now, now.plusDays(2)));
        
        return response;
    }
    
    // Get weekly room availability
    public WeeklyRoomAvailabilityResponse getWeeklyRoomAvailability(Long roomId, LocalDateTime weekStart) {
        Room room = findRoomById(roomId);
        LocalDateTime weekEnd = weekStart.plusWeeks(1);
        
        WeeklyRoomAvailabilityResponse response = new WeeklyRoomAvailabilityResponse();
        response.setRoom(mapRoomToResponse(room));
        response.setWeekStart(weekStart);
        response.setWeekEnd(weekEnd);
        
        List<WeeklyRoomAvailabilityResponse.DayAvailability> dailyAvailability = new ArrayList<>();
        
        for (int i = 0; i < 7; i++) {
            LocalDateTime dayStart = weekStart.plusDays(i);
            LocalDateTime dayEnd = dayStart.plusDays(1);
            
            WeeklyRoomAvailabilityResponse.DayAvailability dayAvail = new WeeklyRoomAvailabilityResponse.DayAvailability();
            dayAvail.setDayOfWeek(dayStart.getDayOfWeek().name());
            dayAvail.setDate(dayStart);
            
            // Check if library is open
            boolean isLibraryOpen = scheduleService.isLibraryOpen(dayStart.toLocalDate(), Location.GISHUSHU);
            dayAvail.setLibraryOpen(isLibraryOpen);
            
            if (isLibraryOpen) {
                dayAvail.setLibraryHours(scheduleService.getLibraryHours(dayStart.toLocalDate(), Location.GISHUSHU));
                dayAvail.setAvailableSlots(generateDailyAvailableSlots(room, dayStart, dayEnd));
                dayAvail.setBookedSlots(generateDailyBookedSlots(room, dayStart, dayEnd));
            }
            
            dailyAvailability.add(dayAvail);
        }
        
        response.setDailyAvailability(dailyAvailability);
        return response;
    }
    
    // Get all rooms real-time availability
    public List<RoomAvailabilityResponse> getAllRoomsAvailability() {
        List<Room> rooms = roomRepository.findByAvailableTrue();
        return rooms.stream()
                .map(room -> getRoomAvailability(room.getId()))
                .collect(Collectors.toList());
    }
    
    private List<RoomAvailabilityResponse.TimeSlot> generateAvailableSlots(Room room, LocalDateTime start, LocalDateTime end) {
        List<RoomAvailabilityResponse.TimeSlot> slots = new ArrayList<>();
        List<RoomBooking> bookings = roomBookingRepository.findActiveBookingsForRoom(room, start, end);
        
        // Sort bookings by start time
        bookings.sort(Comparator.comparing(RoomBooking::getStartTime));
        
        LocalDateTime currentTime = start;
        
        for (RoomBooking booking : bookings) {
            // Add slot before booking if there's a gap
            if (currentTime.isBefore(booking.getStartTime())) {
                long durationMinutes = ChronoUnit.MINUTES.between(currentTime, booking.getStartTime());
                if (durationMinutes >= 30) { // Minimum 30-minute slots
                    RoomAvailabilityResponse.TimeSlot slot = new RoomAvailabilityResponse.TimeSlot();
                    slot.setStartTime(currentTime);
                    slot.setEndTime(booking.getStartTime());
                    slot.setRecommended(durationMinutes >= room.getMaxBookingHours() * 60);
                    slots.add(slot);
                }
            }
            currentTime = booking.getEndTime();
        }
        
        // Add final slot if there's time remaining
        if (currentTime.isBefore(end)) {
            long durationMinutes = ChronoUnit.MINUTES.between(currentTime, end);
            if (durationMinutes >= 30) {
                RoomAvailabilityResponse.TimeSlot slot = new RoomAvailabilityResponse.TimeSlot();
                slot.setStartTime(currentTime);
                slot.setEndTime(end);
                slot.setRecommended(durationMinutes >= room.getMaxBookingHours() * 60);
                slots.add(slot);
            }
        }
        
        return slots;
    }
    
    private List<RoomAvailabilityResponse.BookingSlot> generateBookedSlots(Room room, LocalDateTime start, LocalDateTime end) {
        List<RoomBooking> bookings = roomBookingRepository.findActiveBookingsForRoom(room, start, end);
        
        return bookings.stream().map(booking -> {
            RoomAvailabilityResponse.BookingSlot slot = new RoomAvailabilityResponse.BookingSlot();
            slot.setStartTime(booking.getStartTime());
            slot.setEndTime(booking.getEndTime());
            slot.setBookedBy(booking.getUser().getFullName());
            slot.setPrivate(!booking.isPublic());
            slot.setCanJoin(booking.isAllowJoining() && booking.isPublic());
            
            if (slot.isCanJoin()) {
                Long acceptedCount = participantRepository.countAcceptedParticipants(booking);
                slot.setAvailableSpots(Math.max(0, booking.getMaxParticipants() - acceptedCount.intValue() - 1));
            }
            
            return slot;
        }).collect(Collectors.toList());
    }
    
    private List<WeeklyRoomAvailabilityResponse.TimeSlot> generateDailyAvailableSlots(Room room, LocalDateTime dayStart, LocalDateTime dayEnd) {
        // Similar to generateAvailableSlots but returns different DTO type
        List<WeeklyRoomAvailabilityResponse.TimeSlot> slots = new ArrayList<>();
        // Implementation similar to above...
        return slots;
    }
    
    private List<WeeklyRoomAvailabilityResponse.BookingSlot> generateDailyBookedSlots(Room room, LocalDateTime dayStart, LocalDateTime dayEnd) {
        List<RoomBooking> bookings = roomBookingRepository.findActiveBookingsForRoom(room, dayStart, dayEnd);
        
        return bookings.stream().map(booking -> {
            WeeklyRoomAvailabilityResponse.BookingSlot slot = new WeeklyRoomAvailabilityResponse.BookingSlot();
            slot.setStartTime(booking.getStartTime());
            slot.setEndTime(booking.getEndTime());
            slot.setTitle(booking.getTitle());
            slot.setPrivate(!booking.isPublic());
            slot.setCanJoin(booking.isAllowJoining() && booking.isPublic());
            
            if (slot.isCanJoin()) {
                Long acceptedCount = participantRepository.countAcceptedParticipants(booking);
                slot.setAvailableSpots(Math.max(0, booking.getMaxParticipants() - acceptedCount.intValue() - 1));
            }
            
            return slot;
        }).collect(Collectors.toList());
    }
    
    private Room findRoomById(Long roomId) {
        return roomRepository.findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found: " + roomId));
    }

    private RoomResponse mapRoomToResponse(Room room) {
    RoomResponse response = new RoomResponse();
    response.setId(room.getId());
    response.setName(room.getName());
    response.setRoomNumber(room.getRoomNumber());
    response.setCapacity(room.getCapacity());
    response.setCategory(room.getCategory());
    response.setBuilding(room.getBuilding());
    response.setFloor(room.getFloor());
    response.setAvailable(room.isAvailable());
    // Add other room fields as needed
    return response;
}
}
