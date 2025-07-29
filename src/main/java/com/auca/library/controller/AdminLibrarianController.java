package com.auca.library.controller;

import com.auca.library.dto.request.CreateLibrarianRequest;
import com.auca.library.dto.request.LibrarianRequest;
import com.auca.library.dto.response.LibrarianResponse;
import com.auca.library.dto.response.MessageResponse;
import com.auca.library.model.Location;
import com.auca.library.service.LibrarianService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.DayOfWeek;
import java.util.List;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/admin/librarians")
@PreAuthorize("hasRole('ADMIN')")
public class AdminLibrarianController {

    @Autowired
    private LibrarianService librarianService;

    // Get all librarians across all locations
    @GetMapping
    public ResponseEntity<List<LibrarianResponse>> getAllLibrarians() {
        return ResponseEntity.ok(librarianService.getAllLibrarians());
    }

    // Get librarians by location
    @GetMapping("/location/{location}")
    public ResponseEntity<List<LibrarianResponse>> getLibrariansByLocation(
            @PathVariable Location location) {
        return ResponseEntity.ok(librarianService.getLibrariansByLocation(location));
    }

    // Get librarians scheduled for a specific day and location
    @GetMapping("/schedule")
    public ResponseEntity<List<LibrarianResponse>> getLibrariansForDay(
            @RequestParam DayOfWeek dayOfWeek,
            @RequestParam Location location) {
        return ResponseEntity.ok(librarianService.getLibrariansForDay(dayOfWeek, location));
    }

    // Get weekly schedule for a location
    @GetMapping("/schedule/weekly/{location}")
    public ResponseEntity<List<LibrarianResponse>> getWeeklySchedule(
            @PathVariable Location location) {
        return ResponseEntity.ok(librarianService.getWeeklySchedule(location));
    }

    // Create new librarian user
    @PostMapping
    public ResponseEntity<LibrarianResponse> createLibrarian(
            @Valid @RequestBody CreateLibrarianRequest request) {
        return ResponseEntity.ok(librarianService.createLibrarian(request));
    }

    // Update librarian schedule
    @PutMapping("/schedule")
    public ResponseEntity<LibrarianResponse> updateLibrarianSchedule(
            @Valid @RequestBody LibrarianRequest request) {
        return ResponseEntity.ok(librarianService.updateLibrarianSchedule(request));
    }

    // Toggle librarian active status
    @PutMapping("/{id}/toggle-active")
    public ResponseEntity<MessageResponse> toggleLibrarianActiveStatus(@PathVariable Long id) {
        return ResponseEntity.ok(librarianService.toggleLibrarianActiveStatus(id));
    }
}