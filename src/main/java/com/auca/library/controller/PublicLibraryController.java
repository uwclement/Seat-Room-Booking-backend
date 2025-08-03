package com.auca.library.controller;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
@RequestMapping("/api/library")
public class PublicLibraryController {

    @Autowired
    private LibraryScheduleService libraryScheduleService;
    
    @Autowired
    private UserService userService;

    // Get current week's schedule
    @GetMapping("/schedule")
    public ResponseEntity<List<LibraryScheduleResponse>> getCurrentSchedule() {
        return ResponseEntity.ok(libraryScheduleService.getAllLibrarySchedules());
    }

     private User getCurrentUser(Authentication auth) {
        return userService.findByEmail(auth.getName()).orElseThrow();
    }

    //  @GetMapping("/status/{location}")
    // public ResponseEntity<LibraryStatusResponse> getLibraryStatusByLocation(@PathVariable Location location) {
    //     return ResponseEntity.ok(libraryScheduleService.getCurrentLibraryStatus(location));
    // }

    //  @GetMapping("/status")
    // public ResponseEntity<LibraryStatusResponse> getCurrentLibraryStatus(Authentication auth) {
    //     if (auth == null || !auth.isAuthenticated()) {
    //         // For unauthenticated users, you might want to default to one location
    //         // or return an error. For now, defaulting to GISHUSHU
    //         return ResponseEntity.ok(libraryScheduleService.getCurrentLibraryStatus(Location.GISHUSHU));
    //     }
        
    //     User currentUser = getCurrentUser(auth);
    //     return ResponseEntity.ok(libraryScheduleService.getCurrentLibraryStatus(currentUser.getLocation()));
    // }


    // Get library schedules (all for students, location-specific for librarian)
    @GetMapping
    public ResponseEntity<List<LibraryScheduleResponse>> getAllSchedules(Authentication auth) {
        User currentUser = getCurrentUser(auth);
        
        if (currentUser.isStudent()) {
            return ResponseEntity.ok(libraryScheduleService.getAllLibrarySchedules());
        } else {
            // Librarian sees only their location schedules
            return ResponseEntity.ok(libraryScheduleService.getLibrarySchedulesByLocation(currentUser.getLocation()));
        }
    }

    // Get schedules for specific location (admin/Students only)
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


    // Get current library status for user's location
    @GetMapping("/status")
    public ResponseEntity<LibraryStatusResponse> getCurrentStatus(Authentication auth) {
        User currentUser = getCurrentUser(auth);
        return ResponseEntity.ok(libraryScheduleService.getCurrentLibraryStatus(currentUser.getLocation()));
    }

    // Get current library status for specific location (admin only)
    @GetMapping("/status/{location}")
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


    // Get current schedule message
    @GetMapping("/message")
    public ResponseEntity<MessageResponse> getScheduleMessage() {
        String message = libraryScheduleService.getScheduleMessage();
        return ResponseEntity.ok(new MessageResponse(message != null ? message : ""));
    }
}