package com.auca.library.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.auca.library.dto.request.RoomBookingRequest;
import com.auca.library.exception.BookingConflictException;
import com.auca.library.exception.BookingLimitExceededException;
import com.auca.library.model.Room;
import com.auca.library.model.RoomBooking;
import com.auca.library.model.User;
import com.auca.library.model.Seat;
import com.auca.library.repository.RecurringBookingSeriesRepository;
import com.auca.library.repository.RoomBookingRepository;

@Service
public class BookingValidationService {
    
    @Autowired private RoomBookingRepository roomBookingRepository;
    @Autowired private LibraryScheduleService libraryScheduleService;
    @Autowired private RecurringBookingSeriesRepository recurringSeriesRepository;
    
    public void validateBookingRequest(RoomBookingRequest request, User user, Room room, Seat seat) {
        validateBookingTimes(request, room);
        validateLibrarySchedule(request, seat);
        validateUserBookingLimits(request, user, room);
        validateRoomCapacity(request, room);
        validateBookingWindow(request);
        validateRecurringBookingLimits(request, user);
    }
    
    private void validateBookingTimes(RoomBookingRequest request, Room room) {
        LocalDateTime startTime = request.getStartTime();
        LocalDateTime endTime = request.getEndTime();
        
        // Basic time validation
        if (startTime.isAfter(endTime)) {
            throw new IllegalArgumentException("Start time must be before end time");
        }
        
        if (startTime.isBefore(LocalDateTime.now().plusMinutes(15))) {
            throw new IllegalArgumentException("Booking must be at least 15 minutes in the future");
        }
        
        // Duration validation
        long durationHours = ChronoUnit.HOURS.between(startTime, endTime);
        if (durationHours > room.getMaxBookingHours()) {
            throw new BookingLimitExceededException(
                String.format("Booking duration cannot exceed %d hours for this room", room.getMaxBookingHours())
            );
        }
        
        if (durationHours < 1) {
            throw new IllegalArgumentException("Minimum booking duration is 1 hour");
        }
        
        // Validate booking is within same day (optional business rule)
        if (!startTime.toLocalDate().equals(endTime.toLocalDate())) {
            throw new IllegalArgumentException("Booking cannot span multiple days");
        }
    }
    
    private void validateLibrarySchedule(RoomBookingRequest request, Seat seat) {
        LocalDate bookingDate = request.getStartTime().toLocalDate();
        
        // Check if library is open on booking date
        if (!libraryScheduleService.isLibraryOpenAt(bookingDate, request.getStartTime().toLocalTime(), seat.getLocation() )) {
            throw new BookingConflictException("Library is closed at the requested start time");
        }
        
        if (!libraryScheduleService.isLibraryOpenAt(bookingDate, request.getEndTime().toLocalTime(), seat.getLocation())) {
            throw new BookingConflictException("Library is closed at the requested end time");
        }
        
        // Validate entire booking period is within library hours
        if (!libraryScheduleService.isValidBookingTime(
                bookingDate, 
                request.getStartTime().toLocalTime(), 
                request.getEndTime().toLocalTime(), seat.getLocation() )) {
            throw new BookingConflictException("Booking time is outside library operating hours");
        }
    }
    
    private void validateUserBookingLimits(RoomBookingRequest request, User user, Room room) {
        LocalDate bookingDate = request.getStartTime().toLocalDate();
        
        // Daily booking limit - FIXED: Convert LocalDate to LocalDateTime
        Long dailyBookings = roomBookingRepository.countUserBookingsForDate(user, bookingDate.atStartOfDay());
        if (dailyBookings >= room.getMaxBookingsPerDay()) {
            throw new BookingLimitExceededException(
                String.format("User has reached daily booking limit of %d for this room", room.getMaxBookingsPerDay())
            );
        }
        
        // Weekly booking limit (3 hours total per week)
        LocalDateTime weekStart = getWeekStart(bookingDate);
        LocalDateTime weekEnd = weekStart.plusWeeks(1);
        
        List<RoomBooking> weeklyBookings = roomBookingRepository.findByUserAndStartTimeBetweenAndStatusIn(
            user, weekStart, weekEnd, 
            List.of(RoomBooking.BookingStatus.CONFIRMED, RoomBooking.BookingStatus.CHECKED_IN)
        );
        
        long totalWeeklyHours = weeklyBookings.stream()
            .mapToLong(booking -> ChronoUnit.HOURS.between(booking.getStartTime(), booking.getEndTime()))
            .sum();
        
        long requestedHours = ChronoUnit.HOURS.between(request.getStartTime(), request.getEndTime());
        
        if (totalWeeklyHours + requestedHours > 3) {
            throw new BookingLimitExceededException(
                String.format("Weekly booking limit of 3 hours exceeded. Current: %d hours, Requested: %d hours", 
                totalWeeklyHours, requestedHours)
            );
        }
    }
    
    private void validateRoomCapacity(RoomBookingRequest request, Room room) {
        if (request.getMaxParticipants() > room.getCapacity()) {
            throw new IllegalArgumentException(
                String.format("Requested participants (%d) exceeds room capacity (%d)", 
                request.getMaxParticipants(), room.getCapacity())
            );
        }
    }
    
    private void validateBookingWindow(RoomBookingRequest request) {
        // Rolling weekly booking window - can only book within current week
        LocalDate bookingDate = request.getStartTime().toLocalDate();

        LocalDateTime currentWeekStart = getWeekStart(LocalDate.now());
        LocalDateTime currentWeekEnd = currentWeekStart.plusWeeks(1).minusDays(1); // Saturday

        if (bookingDate.isBefore(currentWeekStart.toLocalDate()) || bookingDate.isAfter(currentWeekEnd.toLocalDate())) {
            throw new BookingConflictException(
                "Bookings can only be made within the current week (Sunday to Saturday)"
            );
        }
    }
    
    private void validateRecurringBookingLimits(RoomBookingRequest request, User user) {
        if (request.isRecurring()) {
            // Check if user has reached recurring booking limit
            Long activeRecurringSeries = recurringSeriesRepository.countActiveSeriesForUser(user);
            if (activeRecurringSeries >= 2) { // Max 2 recurring series per user
                throw new BookingLimitExceededException("User has reached maximum recurring booking series limit");
            }
            
            // Validate recurring booking duration (max 2 hours per session)
            long durationHours = ChronoUnit.HOURS.between(request.getStartTime(), request.getEndTime());
            if (durationHours > 2) {
                throw new BookingLimitExceededException("Recurring bookings cannot exceed 2 hours per session");
            }
        }
    }
    
    private LocalDateTime getWeekStart(LocalDate date) {
        return date.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY)).atStartOfDay();
    }
}