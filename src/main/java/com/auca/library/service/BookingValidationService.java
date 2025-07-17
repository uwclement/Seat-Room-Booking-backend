package com.auca.library.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.auca.library.dto.request.RoomBookingRequest;
import com.auca.library.exception.BookingConflictException;
import com.auca.library.exception.BookingLimitExceededException;
import com.auca.library.model.Role;
import com.auca.library.model.Room;
import com.auca.library.model.RoomBooking;
import com.auca.library.model.User;
import com.auca.library.repository.RecurringBookingSeriesRepository;
import com.auca.library.repository.RoomBookingRepository;

@Service
public class BookingValidationService {
    
    @Autowired private RoomBookingRepository roomBookingRepository;
    @Autowired private LibraryScheduleService libraryScheduleService;
    @Autowired private RecurringBookingSeriesRepository recurringSeriesRepository;
    
    // CONFIGURABLE LIMITS - Add these to application.properties
    @Value("${booking.limits.weekly.hours.student:50}")
    private int studentWeeklyLimit;
    
    @Value("${booking.limits.weekly.hours.professor:50}")
    private int professorWeeklyLimit;
    
    @Value("${booking.limits.weekly.hours.admin:999}")
    private int adminWeeklyLimit;
    
    public void validateBookingRequest(RoomBookingRequest request, User user, Room room) {
        validateBookingTimes(request, room);
        validateLibrarySchedule(request);
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
    
    private void validateLibrarySchedule(RoomBookingRequest request) {
        LocalDate bookingDate = request.getStartTime().toLocalDate();
        
        // Check if library is open on booking date
        if (!libraryScheduleService.isLibraryOpenAt(bookingDate, request.getStartTime().toLocalTime())) {
            throw new BookingConflictException("Library is closed at the requested start time");
        }
        
        if (!libraryScheduleService.isLibraryOpenAt(bookingDate, request.getEndTime().toLocalTime())) {
            throw new BookingConflictException("Library is closed at the requested end time");
        }
        
        // Validate entire booking period is within library hours
        if (!libraryScheduleService.isValidBookingTime(
                bookingDate, 
                request.getStartTime().toLocalTime(), 
                request.getEndTime().toLocalTime())) {
            throw new BookingConflictException("Booking time is outside library operating hours");
        }
    }
    
    // FIXED: Enhanced user booking limits with role-based limits
    private void validateUserBookingLimits(RoomBookingRequest request, User user, Room room) {
        LocalDate bookingDate = request.getStartTime().toLocalDate();
        
        // Daily booking limit
        Long dailyBookings = roomBookingRepository.countUserBookingsForDate(user, bookingDate.atStartOfDay());
        if (dailyBookings >= room.getMaxBookingsPerDay()) {
            throw new BookingLimitExceededException(
                String.format("User has reached daily booking limit of %d for this room", room.getMaxBookingsPerDay())
            );
        }
        
        // FIXED: Role-based weekly booking limits
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
        
        // FIXED: Get appropriate weekly limit based on user role
        int weeklyLimit = getWeeklyLimitForUser(user);
        
        if (totalWeeklyHours + requestedHours > weeklyLimit) {
            throw new BookingLimitExceededException(
                String.format("Weekly booking limit of %d hours exceeded. Current: %d hours, Requested: %d hours", 
                weeklyLimit, totalWeeklyHours, requestedHours)
            );
        }
    }
    
    // NEW: Role-based weekly limits
    private int getWeeklyLimitForUser(User user) {
        // Admin and Librarian - unlimited (or very high limit)
        if (user.hasRole(Role.ERole.ROLE_ADMIN) || user.hasRole(Role.ERole.ROLE_LIBRARIAN)) {
            return adminWeeklyLimit; // 999 hours (effectively unlimited)
        }
        
        // Professor - higher limit
        if (user.hasRole(Role.ERole.ROLE_PROFESSOR) || user.hasRole(Role.ERole.ROLE_HOD)) {
            return professorWeeklyLimit; // 30 hours per week
        }
        
        // Students - standard limit
        return studentWeeklyLimit; // 20 hours per week
    }
    
    private void validateRoomCapacity(RoomBookingRequest request, Room room) {
        if (request.getMaxParticipants() > room.getCapacity()) {
            throw new IllegalArgumentException(
                String.format("Requested participants (%d) exceeds room capacity (%d)", 
                request.getMaxParticipants(), room.getCapacity())
            );
        }
    }
    
    // UPDATED: More flexible booking window
    private void validateBookingWindow(RoomBookingRequest request) {
        LocalDate bookingDate = request.getStartTime().toLocalDate();
        LocalDate today = LocalDate.now();
        
        // Can't book in the past
        if (bookingDate.isBefore(today)) {
            throw new BookingConflictException("Cannot book in the past");
        }
        
        // UPDATED: Allow booking up to 2 weeks in advance (instead of current week only)
        LocalDate maxBookingDate = today.plusWeeks(2);
        if (bookingDate.isAfter(maxBookingDate)) {
            throw new BookingConflictException(
                "Bookings can only be made up to 2 weeks in advance"
            );
        }
    }
    
    private void validateRecurringBookingLimits(RoomBookingRequest request, User user) {
        if (request.isRecurring()) {
            // Check if user has reached recurring booking limit
            Long activeRecurringSeries = recurringSeriesRepository.countActiveSeriesForUser(user);
            int maxRecurringSeries = user.hasRole(Role.ERole.ROLE_ADMIN) ? 10 : 3; // Admins get more
            
            if (activeRecurringSeries >= maxRecurringSeries) {
                throw new BookingLimitExceededException(
                    String.format("User has reached maximum recurring booking series limit of %d", maxRecurringSeries)
                );
            }
            
            // UPDATED: Flexible recurring booking duration based on role
            long durationHours = ChronoUnit.HOURS.between(request.getStartTime(), request.getEndTime());
            int maxRecurringHours = user.hasRole(Role.ERole.ROLE_PROFESSOR) ? 4 : 2; // Professors get longer sessions
            
            if (durationHours > maxRecurringHours) {
                throw new BookingLimitExceededException(
                    String.format("Recurring bookings cannot exceed %d hours per session", maxRecurringHours)
                );
            }
        }
    }
    
    private LocalDateTime getWeekStart(LocalDate date) {
        return date.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY)).atStartOfDay();
    }
}