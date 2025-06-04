package com.auca.library.service;

import com.auca.library.dto.request.RecurringBookingRequest;
import com.auca.library.model.*;
import com.auca.library.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.auca.library.exception.ResourceNotFoundException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class RecurringBookingService {
    
    @Autowired private RecurringBookingSeriesRepository recurringSeriesRepository;
    @Autowired private RoomBookingRepository roomBookingRepository;
    @Autowired private BookingValidationService bookingValidationService;
    @Autowired private NotificationService notificationService;
    
    @Transactional
    public RecurringBookingSeries createRecurringSeries(RoomBooking firstBooking, RecurringBookingRequest recurringDetails) {
        RecurringBookingSeries series = new RecurringBookingSeries();
        series.setUser(firstBooking.getUser());
        series.setRoom(firstBooking.getRoom());
        series.setTitle(firstBooking.getTitle());
        series.setDescription(firstBooking.getDescription());
        series.setRecurrenceType(mapRecurrenceType(recurringDetails.getRecurrenceType()));
        series.setRecurrenceInterval(recurringDetails.getRecurrenceInterval());
        series.setDaysOfWeek(recurringDetails.getDaysOfWeek());
        series.setStartTime(firstBooking.getStartTime().toLocalTime());
        series.setEndTime(firstBooking.getEndTime().toLocalTime());
        series.setSeriesStartDate(recurringDetails.getSeriesStartDate());
        series.setSeriesEndDate(recurringDetails.getSeriesEndDate());
        
        series = recurringSeriesRepository.save(series);
        
        // Link first booking to series
        firstBooking.setRecurringBookingSeries(series);
        roomBookingRepository.save(firstBooking);
        
        // Generate future bookings for next 4 weeks
        generateRecurringBookings(series, LocalDateTime.now().plusWeeks(4));
        
        return series;
    }
    
    @Scheduled(cron = "0 0 1 * * SUN") // Every Sunday at 1 AM - Generate next week's bookings
    @Transactional
    public void generateWeeklyRecurringBookings() {
        LocalDateTime cutoffDate = LocalDateTime.now().plusWeeks(1);
        List<RecurringBookingSeries> series = recurringSeriesRepository.findSeriesNeedingGeneration(cutoffDate);
        
        for (RecurringBookingSeries recurringBookingSeries : series) {
            generateRecurringBookings(recurringBookingSeries, cutoffDate);
        }
    }
    
    private void generateRecurringBookings(RecurringBookingSeries series, LocalDateTime generateUntil) {
        LocalDateTime currentDate = series.getLastGeneratedDate() != null ? 
            series.getLastGeneratedDate().plusDays(1) : series.getSeriesStartDate();
        
        while (currentDate.isBefore(generateUntil) && 
               (series.getSeriesEndDate() == null || currentDate.isBefore(series.getSeriesEndDate()))) {
            
            if (shouldCreateBookingForDate(series, currentDate.toLocalDate())) {
                createRecurringBooking(series, currentDate.toLocalDate());
            }
            
            currentDate = getNextRecurrenceDate(series, currentDate);
        }
        
        series.setLastGeneratedDate(generateUntil.minusDays(1));
        recurringSeriesRepository.save(series);
    }
    
    private boolean shouldCreateBookingForDate(RecurringBookingSeries series, LocalDate date) {
        // Check if day of week matches
        if (!series.getDaysOfWeek().contains(date.getDayOfWeek())) {
            return false;
        }
        
        // Check for conflicts
        LocalDateTime startTime = date.atTime(series.getStartTime());
        LocalDateTime endTime = date.atTime(series.getEndTime());
        
        return roomBookingRepository.countConflictingBookings(series.getRoom(), startTime, endTime) == 0;
    }
    
    private void createRecurringBooking(RecurringBookingSeries series, LocalDate date) {
        try {
            RoomBooking booking = new RoomBooking();
            booking.setRoom(series.getRoom());
            booking.setUser(series.getUser());
            booking.setTitle(series.getTitle());
            booking.setDescription(series.getDescription());
            booking.setStartTime(date.atTime(series.getStartTime()));
            booking.setEndTime(date.atTime(series.getEndTime()));
            booking.setMaxParticipants(1); // Default for recurring
            booking.setStatus(RoomBooking.BookingStatus.CONFIRMED);
            booking.setRecurringBookingSeries(series);
            booking.setRequiresApproval(false); // Recurring bookings are pre-approved
            
            roomBookingRepository.save(booking);
            
        } catch (Exception e) {
            // Log error but continue with other bookings
            System.err.println("Failed to create recurring booking for series " + series.getId() + 
                             " on date " + date + ": " + e.getMessage());
        }
    }
    
    private LocalDateTime getNextRecurrenceDate(RecurringBookingSeries series, LocalDateTime currentDate) {
        switch (series.getRecurrenceType()) {
            case DAILY:
                return currentDate.plusDays(series.getRecurrenceInterval());
            case WEEKLY:
                return currentDate.plusWeeks(series.getRecurrenceInterval());
            case MONTHLY:
                return currentDate.plusMonths(series.getRecurrenceInterval());
            default:
                return currentDate.plusWeeks(1); // Default to weekly
        }
    }
    
    @Transactional
    public void cancelRecurringSeries(Long seriesId, String userEmail) {
        RecurringBookingSeries series = recurringSeriesRepository.findById(seriesId)
            .orElseThrow(() -> new ResourceNotFoundException("Recurring series not found"));
        
        if (!series.getUser().getEmail().equals(userEmail)) {
            throw new SecurityException("User cannot cancel this recurring series");
        }
        
        // Cancel future bookings
        LocalDateTime now = LocalDateTime.now();
        List<RoomBooking> futureBookings = roomBookingRepository.findByRecurringSeriesAndStartTimeAfter(series, now);
        
        for (RoomBooking booking : futureBookings) {
            if (booking.getStatus() == RoomBooking.BookingStatus.CONFIRMED) {
                booking.setStatus(RoomBooking.BookingStatus.CANCELLED);
                roomBookingRepository.save(booking);
            }
        }
        
        // Mark series as inactive
        series.setActive(false);
        recurringSeriesRepository.save(series);
        
        //notify user
        notificationService.addNotification(
            userEmail,
            "Recurring Booking Series Cancelled",
            String.format("Your recurring booking series '%s' has been cancelled", series.getTitle()),
            "BOOKING_CANCELLED"
        );
    }
    
    private RecurringBookingSeries.RecurrenceType mapRecurrenceType(RecurringBookingRequest.RecurrenceType requestType) {
        switch (requestType) {
            case DAILY: return RecurringBookingSeries.RecurrenceType.DAILY;
            case WEEKLY: return RecurringBookingSeries.RecurrenceType.WEEKLY;
            case MONTHLY: return RecurringBookingSeries.RecurrenceType.MONTHLY;
            case CUSTOM: return RecurringBookingSeries.RecurrenceType.CUSTOM;
            default: return RecurringBookingSeries.RecurrenceType.WEEKLY;
        }
    }
}