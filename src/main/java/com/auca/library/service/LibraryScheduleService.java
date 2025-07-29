package com.auca.library.service;

import com.auca.library.dto.request.RecurringClosureRequest;
import com.auca.library.dto.response.LibraryClosureExceptionResponse;
import com.auca.library.dto.response.LibraryScheduleResponse;
import com.auca.library.dto.response.MessageResponse;
import com.auca.library.exception.ResourceNotFoundException;
import com.auca.library.model.LibraryClosureException;
import com.auca.library.model.LibrarySchedule;
import com.auca.library.model.Location;
import com.auca.library.model.User;
import com.auca.library.repository.LibraryClosureExceptionRepository;
import com.auca.library.repository.LibraryScheduleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.auca.library.dto.response.LibraryStatusResponse;

import java.time.LocalDate;
import java.time.LocalDateTime;
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

    // Get schedules for specific location
    public List<LibraryScheduleResponse> getLibrarySchedulesByLocation(Location location) {
        return scheduleRepository.findByLocationOrderByDayOfWeek(location).stream()
                .map(this::mapScheduleToResponse)
                .collect(Collectors.toList());
    }

    // Get all schedules (admin only) - now includes location filtering capability
    public List<LibraryScheduleResponse> getAllLibrarySchedules() {
        return scheduleRepository.findAll().stream()
                .map(this::mapScheduleToResponse)
                .collect(Collectors.toList());
    }

    // Overloaded method for backward compatibility
    public List<LibraryScheduleResponse> getAllLibrarySchedules(Location location) {
        if (location != null) {
            return getLibrarySchedulesByLocation(location);
        }
        return getAllLibrarySchedules();
    }

    @Transactional
    public LibraryScheduleResponse updateLibrarySchedule(Long id, LibraryScheduleResponse scheduleResponse, User currentUser) {
        LibrarySchedule schedule = scheduleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Library schedule not found with id: " + id));

        // Permission check: Admin can edit all, Librarian can edit only their location
        if (!currentUser.isAdmin() && !schedule.getLocation().equals(currentUser.getLocation())) {
            throw new RuntimeException("You can only edit schedules for your library location");
        }

        if (scheduleResponse.getOpenTime() != null) {
            schedule.setOpenTime(LocalTime.parse(scheduleResponse.getOpenTime()));
        }

        if (scheduleResponse.getCloseTime() != null) {
            schedule.setCloseTime(LocalTime.parse(scheduleResponse.getCloseTime()));
        }

        if (scheduleResponse.getIsOpen() != null) {
            schedule.setOpen(scheduleResponse.getIsOpen());
        }

        if (scheduleResponse.getSpecialCloseTime() != null) {
            schedule.setSpecialCloseTime(LocalTime.parse(scheduleResponse.getSpecialCloseTime()));
        }

        if (scheduleResponse.getMessage() != null) {
            schedule.setMessage(scheduleResponse.getMessage());
        }

        schedule.setLastModified(LocalDateTime.now());
        schedule = scheduleRepository.save(schedule);

        return mapScheduleToResponse(schedule);
    }

    @Transactional
    public LibraryScheduleResponse setDayClosed(Long id, String message, User currentUser) {
        LibrarySchedule schedule = scheduleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Library schedule not found with id: " + id));

        // Permission check
        if (!currentUser.isAdmin() && !schedule.getLocation().equals(currentUser.getLocation())) {
            throw new RuntimeException("You can only edit schedules for your library location");
        }

        schedule.setOpen(false);
        schedule.setMessage(message);
        schedule.setSpecialCloseTime(null);
        schedule.setLastModified(LocalDateTime.now());

        schedule = scheduleRepository.save(schedule);
        return mapScheduleToResponse(schedule);
    }

    @Transactional
    public LibraryScheduleResponse setSpecialClosingTime(Long id, LocalTime specialCloseTime, String message, User currentUser) {
        LibrarySchedule schedule = scheduleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Library schedule not found with id: " + id));

        // Permission check
        if (!currentUser.isAdmin() && !schedule.getLocation().equals(currentUser.getLocation())) {
            throw new RuntimeException("You can only edit schedules for your library location");
        }

        schedule.setSpecialCloseTime(specialCloseTime);
        schedule.setMessage(message);
        schedule.setOpen(true);
        schedule.setLastModified(LocalDateTime.now());

        schedule = scheduleRepository.save(schedule);
        return mapScheduleToResponse(schedule);
    }

    @Transactional
    public LibraryScheduleResponse removeSpecialClosingTime(Long id, User currentUser) {
        LibrarySchedule schedule = scheduleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Library schedule not found with id: " + id));

        // Permission check
        if (!currentUser.isAdmin() && !schedule.getLocation().equals(currentUser.getLocation())) {
            throw new RuntimeException("You can only edit schedules for your library location");
        }

        schedule.setSpecialCloseTime(null);
        schedule.setMessage(null);
        schedule.setLastModified(LocalDateTime.now());

        schedule = scheduleRepository.save(schedule);
        return mapScheduleToResponse(schedule);
    }

    // Get current library status for specific location
    public LibraryStatusResponse getCurrentLibraryStatus(Location location) {
        LocalDateTime now = LocalDateTime.now();
        LocalDate today = now.toLocalDate();
        LocalTime currentTime = now.toLocalTime();

        LibraryStatusResponse status = new LibraryStatusResponse();
        status.setCurrentDateTime(now);

        // Check for closure exceptions first
        LibraryClosureException exception = exceptionRepository.findByDate(today).orElse(null);
        if (exception != null) {
            if (exception.isClosedAllDay()) {
                status.setOpen(false);
                status.setMessage(exception.getReason());
                return status;
            } else if (exception.getOpenTime() != null && exception.getCloseTime() != null) {
                boolean isOpen = !currentTime.isBefore(exception.getOpenTime()) &&
                        currentTime.isBefore(exception.getCloseTime());
                status.setOpen(isOpen);
                status.setCurrentHours(exception.getOpenTime() + " - " + exception.getCloseTime());
                status.setMessage(exception.getReason());
                status.setNextStatusChange(
                        getNextStatusChangeTime(now, exception.getOpenTime(), exception.getCloseTime(), location));
                return status;
            }
        }

        // Check regular schedule for the location
        LibrarySchedule schedule = scheduleRepository.findByDayOfWeekAndLocation(today.getDayOfWeek(), location).orElse(null);
        if (schedule == null) {
            status.setOpen(false);
            status.setMessage("No schedule configured for " + today.getDayOfWeek() + " at " + location.getDisplayName());
            return status;
        }

        if (!schedule.isOpen()) {
            status.setOpen(false);
            status.setMessage(schedule.getMessage() != null ? schedule.getMessage() : "Library closed today");
            return status;
        }

        LocalTime effectiveCloseTime = schedule.getEffectiveCloseTime();
        boolean isOpen = schedule.isOpenAt(currentTime);

        status.setOpen(isOpen);
        status.setCurrentHours(schedule.getOpenTime() + " - " + effectiveCloseTime);
        status.setMessage(schedule.getMessage());
        status.setNextStatusChange(getNextStatusChangeTime(now, schedule.getOpenTime(), effectiveCloseTime, location));

        if (schedule.getSpecialCloseTime() != null) {
            status.setSpecialMessage("Closing early today at " + schedule.getSpecialCloseTime());
        }

        return status;
    }

    // Check if library is open at specific date and time for location
    public boolean isLibraryOpenAt(LocalDate date, LocalTime time, Location location) {
    // Add logging to debug
    System.out.println("Checking library schedule for: " + date.getDayOfWeek() + " at " + location);
    
    // Check for closure exceptions first
    LibraryClosureException exception = exceptionRepository.findByDate(date).orElse(null);
    if (exception != null) {
        System.out.println("Found closure exception: " + exception);
        if (exception.isClosedAllDay()) {
            return false;
        } else if (exception.getOpenTime() != null && exception.getCloseTime() != null) {
            return !time.isBefore(exception.getOpenTime()) && time.isBefore(exception.getCloseTime());
        }
    }

    // Check regular schedule
    LibrarySchedule schedule = scheduleRepository.findByDayOfWeekAndLocation(date.getDayOfWeek(), location).orElse(null);
    System.out.println("Found schedule: " + schedule);
    
    if (schedule == null || !schedule.isOpen()) {
        System.out.println("No schedule found or library is closed on " + date.getDayOfWeek());
        return false;
    }

    boolean isOpen = schedule.isOpenAt(time);
    System.out.println("Is open at " + time + ": " + isOpen);
    return isOpen;
}

    // Validate booking time for specific location
    public boolean isValidBookingTime(LocalDate date, LocalTime startTime, LocalTime endTime, Location location) {
        return isLibraryOpenAt(date, startTime, location) && isLibraryOpenAt(date, endTime, location);
    }

    // Check if library is open on a specific date for location
    public boolean isLibraryOpen(LocalDate date, Location location) {
        // Check for closure exceptions first
        LibraryClosureException exception = exceptionRepository.findByDate(date).orElse(null);
        if (exception != null) {
            return !exception.isClosedAllDay();
        }

        // Check regular schedule
        LibrarySchedule schedule = scheduleRepository.findByDayOfWeekAndLocation(date.getDayOfWeek(), location).orElse(null);
        return schedule != null && schedule.isOpen();
    }

    // Get library operating hours for a specific date and location
    public String getLibraryHours(LocalDate date, Location location) {
        // Check for closure exceptions first
        LibraryClosureException exception = exceptionRepository.findByDate(date).orElse(null);
        if (exception != null) {
            if (exception.isClosedAllDay()) {
                return "Closed";
            } else if (exception.getOpenTime() != null && exception.getCloseTime() != null) {
                return exception.getOpenTime().format(DateTimeFormatter.ofPattern("HH:mm")) + 
                       " - " + 
                       exception.getCloseTime().format(DateTimeFormatter.ofPattern("HH:mm"));
            }
        }

        // Check regular schedule
        LibrarySchedule schedule = scheduleRepository.findByDayOfWeekAndLocation(date.getDayOfWeek(), location).orElse(null);
        if (schedule == null || !schedule.isOpen()) {
            return "Closed";
        }

        LocalTime effectiveCloseTime = schedule.getEffectiveCloseTime();
        return schedule.getOpenTime().format(DateTimeFormatter.ofPattern("HH:mm")) + 
               " - " + 
               effectiveCloseTime.format(DateTimeFormatter.ofPattern("HH:mm"));
    }

    // Helper method to calculate next status change time
    private LocalDateTime getNextStatusChangeTime(LocalDateTime now, LocalTime openTime, LocalTime closeTime, Location location) {
        LocalDate today = now.toLocalDate();
        LocalTime currentTime = now.toLocalTime();

        if (currentTime.isBefore(openTime)) {
            return today.atTime(openTime);
        } else if (currentTime.isBefore(closeTime)) {
            return today.atTime(closeTime);
        } else {
            LibrarySchedule nextSchedule = scheduleRepository.findByDayOfWeekAndLocation(
                    today.plusDays(1).getDayOfWeek(), location).orElse(null);

            if (nextSchedule != null && nextSchedule.isOpen()) {
                return today.plusDays(1).atTime(nextSchedule.getOpenTime());
            }

            // Find next open day for this location
            for (int i = 1; i <= 7; i++) {
                LocalDate nextDay = today.plusDays(i);
                LibrarySchedule schedule = scheduleRepository.findByDayOfWeekAndLocation(nextDay.getDayOfWeek(), location)
                        .orElse(null);
                if (schedule != null && schedule.isOpen()) {
                    return nextDay.atTime(schedule.getOpenTime());
                }
            }

            return null; // No opening scheduled
        }
    }

    // Enhanced mapping method that includes location
    private LibraryScheduleResponse mapScheduleToResponse(LibrarySchedule schedule) {
        LibraryScheduleResponse response = new LibraryScheduleResponse();
        response.setId(schedule.getId());
        response.setDayOfWeek(schedule.getDayOfWeek().toString());
        response.setOpenTime(schedule.getOpenTime().format(DateTimeFormatter.ISO_LOCAL_TIME));
        response.setCloseTime(schedule.getCloseTime().format(DateTimeFormatter.ISO_LOCAL_TIME));
        response.setIsOpen(schedule.isOpen());
        response.setLocation(schedule.getLocation().toString()); // Add location to response

        if (schedule.getSpecialCloseTime() != null) {
            response.setSpecialCloseTime(schedule.getSpecialCloseTime().format(DateTimeFormatter.ISO_LOCAL_TIME));
        }

        response.setMessage(schedule.getMessage());
        response.setLastModified(schedule.getLastModified());

        return response;
    }

    // Other existing methods remain the same...
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
    public LibraryClosureExceptionResponse updateClosureException(Long id,
            LibraryClosureExceptionResponse exceptionResponse) {
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
                if (currentDate.getDayOfWeek() != recurringRequest.getDayOfWeek()) {
                    currentDate = currentDate.with(TemporalAdjusters.next(recurringRequest.getDayOfWeek()));
                    if (currentDate.isAfter(endDate)) {
                        break;
                    }
                }
                dateToAdd = currentDate;
                currentDate = currentDate.plusWeeks(1);
            } else if (recurringRequest.getDayOfMonth() != null) {
                if (currentDate.getDayOfMonth() != recurringRequest.getDayOfMonth()) {
                    try {
                        currentDate = currentDate.withDayOfMonth(recurringRequest.getDayOfMonth());
                    } catch (Exception e) {
                        currentDate = currentDate.plusMonths(1).withDayOfMonth(1);
                        try {
                            currentDate = currentDate.withDayOfMonth(recurringRequest.getDayOfMonth());
                        } catch (Exception ex) {
                            currentDate = currentDate.plusMonths(1).withDayOfMonth(1);
                            continue;
                        }
                    }

                    if (currentDate.isAfter(endDate)) {
                        break;
                    }
                }
                dateToAdd = currentDate;
                currentDate = currentDate.plusMonths(1);
            } else {
                dateToAdd = currentDate;
                currentDate = currentDate.plusDays(1);
            }

            if (dateToAdd != null) {
                LibraryClosureException exception = new LibraryClosureException();
                exception.setDate(dateToAdd);
                exception.setClosedAllDay(recurringRequest.isClosedAllDay());
                exception.setReason(recurringRequest.getReason());

                if (!recurringRequest.isClosedAllDay() && recurringRequest.getOpenTime() != null
                        && recurringRequest.getCloseTime() != null) {
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

    private void updateExceptionFromResponse(LibraryClosureException exception,
            LibraryClosureExceptionResponse response) {
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