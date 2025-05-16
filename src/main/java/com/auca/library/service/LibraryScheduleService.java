package com.auca.library.service;

import com.auca.library.dto.request.RecurringClosureRequest;
import com.auca.library.dto.response.LibraryClosureExceptionResponse;
import com.auca.library.dto.response.LibraryScheduleResponse;
import com.auca.library.dto.response.MessageResponse;
import com.auca.library.exception.ResourceNotFoundException;
import com.auca.library.model.LibraryClosureException;
import com.auca.library.model.LibrarySchedule;
import com.auca.library.repository.LibraryClosureExceptionRepository;
import com.auca.library.repository.LibraryScheduleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class LibraryScheduleService {

    @Autowired
    private LibraryScheduleRepository scheduleRepository;

    @Autowired
    private LibraryClosureExceptionRepository exceptionRepository;

    private String scheduleMessage;

    public List<LibraryScheduleResponse> getAllLibrarySchedules() {
        return scheduleRepository.findAll().stream()
                .map(this::mapScheduleToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public LibraryScheduleResponse updateLibrarySchedule(Long id, LibraryScheduleResponse scheduleResponse) {
        LibrarySchedule schedule = scheduleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Library schedule not found with id: " + id));
        
        if (scheduleResponse.getOpenTime() != null) {
            schedule.setOpenTime(LocalTime.parse(scheduleResponse.getOpenTime()));
        }
        
        if (scheduleResponse.getCloseTime() != null) {
            schedule.setCloseTime(LocalTime.parse(scheduleResponse.getCloseTime()));
        }
        
        schedule = scheduleRepository.save(schedule);
        return mapScheduleToResponse(schedule);
    }

    public List<LibraryClosureExceptionResponse> getAllClosureExceptions() {
        return exceptionRepository.findAll().stream()
                .map(this::mapExceptionToResponse)
                .collect(Collectors.toList());
    }

    public List<LibraryClosureExceptionResponse> getClosureExceptionsInRange(LocalDate start, LocalDate end) {
        return exceptionRepository.findByDateBetween(start, end).stream()
                .map(this::mapExceptionToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public LibraryClosureExceptionResponse createClosureException(LibraryClosureExceptionResponse exceptionResponse) {
        LibraryClosureException exception = new LibraryClosureException();
        updateExceptionFromResponse(exception, exceptionResponse);
        exception = exceptionRepository.save(exception);
        return mapExceptionToResponse(exception);
    }

    @Transactional
    public LibraryClosureExceptionResponse updateClosureException(Long id, LibraryClosureExceptionResponse exceptionResponse) {
        LibraryClosureException exception = exceptionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Closure exception not found with id: " + id));
        
        updateExceptionFromResponse(exception, exceptionResponse);
        exception = exceptionRepository.save(exception);
        return mapExceptionToResponse(exception);
    }

    @Transactional
    public MessageResponse deleteClosureException(Long id) {
        if (!exceptionRepository.existsById(id)) {
            throw new ResourceNotFoundException("Closure exception not found with id: " + id);
        }
        exceptionRepository.deleteById(id);
        return new MessageResponse("Closure exception deleted successfully");
    }

    @Transactional
    public List<LibraryClosureExceptionResponse> createRecurringClosures(RecurringClosureRequest recurringRequest) {
        List<LibraryClosureException> exceptions = new ArrayList<>();
        LocalDate currentDate = recurringRequest.getStartDate();
        LocalDate endDate = recurringRequest.getEndDate();
        
        while (!currentDate.isAfter(endDate)) {
            LocalDate dateToAdd = null;
            
            if (recurringRequest.getDayOfWeek() != null) {
                // If we're looking for a specific day of the week
                if (currentDate.getDayOfWeek() != recurringRequest.getDayOfWeek()) {
                    // Move to the next occurrence of that day of week
                    currentDate = currentDate.with(TemporalAdjusters.next(recurringRequest.getDayOfWeek()));
                    if (currentDate.isAfter(endDate)) {
                        break;
                    }
                }
                dateToAdd = currentDate;
                // Move to the next week for the next iteration
                currentDate = currentDate.plusWeeks(1);
            } else if (recurringRequest.getDayOfMonth() != null) {
                // If we're looking for a specific day of the month
                if (currentDate.getDayOfMonth() != recurringRequest.getDayOfMonth()) {
                    // Try to move to that day in the current month
                    try {
                        currentDate = currentDate.withDayOfMonth(recurringRequest.getDayOfMonth());
                    } catch (Exception e) {
                        // If that day doesn't exist in the current month, move to the next month
                        currentDate = currentDate.plusMonths(1).withDayOfMonth(1);
                        try {
                            currentDate = currentDate.withDayOfMonth(recurringRequest.getDayOfMonth());
                        } catch (Exception ex) {
                            // If still not possible, skip this month
                            currentDate = currentDate.plusMonths(1).withDayOfMonth(1);
                            continue;
                        }
                    }
                    
                    if (currentDate.isAfter(endDate)) {
                        break;
                    }
                }
                dateToAdd = currentDate;
                // Move to the next month for the next iteration
                currentDate = currentDate.plusMonths(1);
            } else {
                // If no specific recurrence pattern, just add the current date and move to the next day
                dateToAdd = currentDate;
                currentDate = currentDate.plusDays(1);
            }
            
            if (dateToAdd != null) {
                LibraryClosureException exception = new LibraryClosureException();
                exception.setDate(dateToAdd);
                exception.setClosedAllDay(recurringRequest.isClosedAllDay());
                exception.setReason(recurringRequest.getReason());
                
                if (!recurringRequest.isClosedAllDay() && recurringRequest.getOpenTime() != null && recurringRequest.getCloseTime() != null) {
                    exception.setOpenTime(LocalTime.parse(recurringRequest.getOpenTime()));
                    exception.setCloseTime(LocalTime.parse(recurringRequest.getCloseTime()));
                }
                
                exceptions.add(exception);
            }
        }
        
        exceptions = exceptionRepository.saveAll(exceptions);
        return exceptions.stream()
                .map(this::mapExceptionToResponse)
                .collect(Collectors.toList());
    }

    public void setScheduleMessage(String message) {
        this.scheduleMessage = message;
    }

    public String getScheduleMessage() {
        return this.scheduleMessage;
    }

    // Helper methods for conversion between responses and entities
    private LibraryScheduleResponse mapScheduleToResponse(LibrarySchedule schedule) {
        LibraryScheduleResponse response = new LibraryScheduleResponse();
        response.setId(schedule.getId());
        response.setDayOfWeek(schedule.getDayOfWeek().toString());
        response.setOpenTime(schedule.getOpenTime().format(DateTimeFormatter.ISO_LOCAL_TIME));
        response.setCloseTime(schedule.getCloseTime().format(DateTimeFormatter.ISO_LOCAL_TIME));
        return response;
    }

    private LibraryClosureExceptionResponse mapExceptionToResponse(LibraryClosureException exception) {
        LibraryClosureExceptionResponse response = new LibraryClosureExceptionResponse();
        response.setId(exception.getId());
        response.setDate(exception.getDate().toString());
        response.setClosedAllDay(exception.isClosedAllDay());
        response.setReason(exception.getReason());
        
        if (exception.getOpenTime() != null) {
            response.setOpenTime(exception.getOpenTime().format(DateTimeFormatter.ISO_LOCAL_TIME));
        }
        
        if (exception.getCloseTime() != null) {
            response.setCloseTime(exception.getCloseTime().format(DateTimeFormatter.ISO_LOCAL_TIME));
        }
        
        return response;
    }

    private void updateExceptionFromResponse(LibraryClosureException exception, LibraryClosureExceptionResponse response) {
        if (response.getDate() != null) {
            exception.setDate(LocalDate.parse(response.getDate()));
        }
        
        if (response.getClosedAllDay() != null) {
            exception.setClosedAllDay(response.getClosedAllDay());
        }
        
        if (response.getReason() != null) {
            exception.setReason(response.getReason());
        }
        
        if (!exception.isClosedAllDay()) {
            if (response.getOpenTime() != null) {
                exception.setOpenTime(LocalTime.parse(response.getOpenTime()));
            }
            
            if (response.getCloseTime() != null) {
                exception.setCloseTime(LocalTime.parse(response.getCloseTime()));
            }
        } else {
            exception.setOpenTime(null);
            exception.setCloseTime(null);
        }
    }
}