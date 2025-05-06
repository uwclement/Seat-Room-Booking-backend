package com.auca.library.controller;

import com.auca.library.dto.request.RecurringClosureRequest;
import com.auca.library.dto.response.LibraryClosureExceptionResponse;
import com.auca.library.dto.response.LibraryScheduleResponse;
import com.auca.library.dto.response.MessageResponse;
import com.auca.library.service.LibraryScheduleService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/admin/schedule")
@PreAuthorize("hasRole('ADMIN')")
public class AdminLibraryScheduleController {

    @Autowired
    private LibraryScheduleService libraryScheduleService;

    // Get all regular library schedules
    @GetMapping
    public ResponseEntity<List<LibraryScheduleResponse>> getAllSchedules() {
        return ResponseEntity.ok(libraryScheduleService.getAllLibrarySchedules());
    }

    // Update a regular day schedule
    @PutMapping("/{id}")
    public ResponseEntity<LibraryScheduleResponse> updateSchedule(
            @PathVariable Long id,
            @Valid @RequestBody LibraryScheduleResponse scheduleResponse) {
        return ResponseEntity.ok(libraryScheduleService.updateLibrarySchedule(id, scheduleResponse));
    }

    // Get all closure exceptions
    @GetMapping("/exceptions")
    public ResponseEntity<List<LibraryClosureExceptionResponse>> getAllClosureExceptions() {
        return ResponseEntity.ok(libraryScheduleService.getAllClosureExceptions());
    }

    // Get closure exceptions for a date range
    @GetMapping("/exceptions/range")
    public ResponseEntity<List<LibraryClosureExceptionResponse>> getClosureExceptionsInRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        return ResponseEntity.ok(libraryScheduleService.getClosureExceptionsInRange(start, end));
    }

    // Create a new closure exception
    @PostMapping("/exceptions")
    public ResponseEntity<LibraryClosureExceptionResponse> createClosureException(
            @Valid @RequestBody LibraryClosureExceptionResponse exceptionResponse) {
        return ResponseEntity.ok(libraryScheduleService.createClosureException(exceptionResponse));
    }

    // Update a closure exception
    @PutMapping("/exceptions/{id}")
    public ResponseEntity<LibraryClosureExceptionResponse> updateClosureException(
            @PathVariable Long id,
            @Valid @RequestBody LibraryClosureExceptionResponse exceptionResponse) {
        return ResponseEntity.ok(libraryScheduleService.updateClosureException(id, exceptionResponse));
    }

    // Delete a closure exception
    @DeleteMapping("/exceptions/{id}")
    public ResponseEntity<MessageResponse> deleteClosureException(@PathVariable Long id) {
        return ResponseEntity.ok(libraryScheduleService.deleteClosureException(id));
    }

    // Create recurring closures (e.g., every Sunday for the next 3 months)
    @PostMapping("/exceptions/recurring")
    public ResponseEntity<List<LibraryClosureExceptionResponse>> createRecurringClosures(
            @Valid @RequestBody RecurringClosureRequest recurringClosureRequest) {
        return ResponseEntity.ok(libraryScheduleService.createRecurringClosures(recurringClosureRequest));
    }

    // Set early closing message
    @PutMapping("/message")
    public ResponseEntity<MessageResponse> setScheduleMessage(
            @RequestBody Map<String, String> messagePayload) {
        String message = messagePayload.get("message");
        libraryScheduleService.setScheduleMessage(message);
        return ResponseEntity.ok(new MessageResponse("Schedule message updated successfully"));
    }
    
    // Get current schedule message
    @GetMapping("/message")
    public ResponseEntity<MessageResponse> getScheduleMessage() {
        String message = libraryScheduleService.getScheduleMessage();
        return ResponseEntity.ok(new MessageResponse(message != null ? message : ""));
    }
}