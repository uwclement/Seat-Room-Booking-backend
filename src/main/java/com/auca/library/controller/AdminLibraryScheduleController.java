package com.auca.library.controller;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.auca.library.dto.request.RecurringClosureRequest;
import com.auca.library.dto.response.LibraryClosureExceptionResponse;
import com.auca.library.dto.response.LibraryScheduleResponse;
import com.auca.library.dto.response.LibraryStatusResponse;
import com.auca.library.dto.response.MessageResponse;
import com.auca.library.model.Location;
import com.auca.library.model.User;
import com.auca.library.service.LibraryScheduleService;
import com.auca.library.service.UserService;

import jakarta.validation.Valid;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/admin/schedule")
@PreAuthorize("hasRole('ADMIN') or hasRole('LIBRARIAN')")
public class AdminLibraryScheduleController {

    @Autowired
    private LibraryScheduleService libraryScheduleService;

    @Autowired
    private UserService userService;

    // Helper method to get current user
    private User getCurrentUser(Authentication auth) {
        // Replace this with your existing method to get current user
        // For example: return userService.getCurrentUser(auth);
        // or: return (User) auth.getPrincipal();
        // or: return userService.findByUsername(auth.getName());
        
        // Temporary implementation - replace with your actual method
        return userService.findByEmail(auth.getName()).orElseThrow();
    }

    // Get library schedules (all for admin, location-specific for librarian)
    @GetMapping
    public ResponseEntity<List<LibraryScheduleResponse>> getAllSchedules(Authentication auth) {
        User currentUser = getCurrentUser(auth);
        
        if (currentUser.isAdmin()) {
            return ResponseEntity.ok(libraryScheduleService.getAllLibrarySchedules());
        } else {
            // Librarian sees only their location schedules
            return ResponseEntity.ok(libraryScheduleService.getLibrarySchedulesByLocation(currentUser.getLocation()));
        }
    }

    // Get schedules for specific location (admin only)
    @GetMapping("/location/{location}")
    public ResponseEntity<List<LibraryScheduleResponse>> getSchedulesByLocation(@PathVariable Location location) {
        return ResponseEntity.ok(libraryScheduleService.getLibrarySchedulesByLocation(location));
    }

    // Update a regular day schedule
    @PutMapping("/{id}")
    public ResponseEntity<LibraryScheduleResponse> updateSchedule(
            @PathVariable Long id,
            @Valid @RequestBody LibraryScheduleResponse scheduleResponse,
            Authentication auth) {
        User currentUser = getCurrentUser(auth);
        return ResponseEntity.ok(libraryScheduleService.updateLibrarySchedule(id, scheduleResponse, currentUser));
    }

    // Mark a day as completely closed
    @PutMapping("/{id}/close")
    public ResponseEntity<LibraryScheduleResponse> setDayClosed(
            @PathVariable Long id,
            @RequestBody Map<String, String> requestBody,
            Authentication auth) {
        String message = requestBody.getOrDefault("message", "Library closed");
        User currentUser = getCurrentUser(auth);
        return ResponseEntity.ok(libraryScheduleService.setDayClosed(id, message, currentUser));
    }

    // Set special closing time for a day
    @PutMapping("/{id}/special-close")
    public ResponseEntity<LibraryScheduleResponse> setSpecialClosingTime(
            @PathVariable Long id,
            @RequestBody Map<String, String> requestBody,
            Authentication auth) {
        String timeStr = requestBody.get("specialCloseTime");
        String message = requestBody.getOrDefault("message", "Early closing today");

        LocalTime specialCloseTime = LocalTime.parse(timeStr);
        User currentUser = getCurrentUser(auth);
        return ResponseEntity.ok(libraryScheduleService.setSpecialClosingTime(id, specialCloseTime, message, currentUser));
    }

    // Remove special closing time
    @DeleteMapping("/{id}/special-close")
    public ResponseEntity<LibraryScheduleResponse> removeSpecialClosingTime(
            @PathVariable Long id, 
            Authentication auth) {
        User currentUser = getCurrentUser(auth);
        return ResponseEntity.ok(libraryScheduleService.removeSpecialClosingTime(id, currentUser));
    }

    // Get current library status for user's location
    @GetMapping("/status")
    public ResponseEntity<LibraryStatusResponse> getCurrentStatus(Authentication auth) {
        User currentUser = getCurrentUser(auth);
        return ResponseEntity.ok(libraryScheduleService.getCurrentLibraryStatus(currentUser.getLocation()));
    }

    // Get current library status for specific location (admin only)
    @GetMapping("/status/{location}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<LibraryStatusResponse> getCurrentStatusByLocation(@PathVariable Location location) {
        return ResponseEntity.ok(libraryScheduleService.getCurrentLibraryStatus(location));
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

    // Create recurring closures
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