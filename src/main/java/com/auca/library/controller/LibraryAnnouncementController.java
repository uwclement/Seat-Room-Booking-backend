package com.auca.library.controller;

import com.auca.library.dto.request.LibraryAnnouncementRequest;
import com.auca.library.dto.response.LibraryAnnouncementResponse;
import com.auca.library.dto.response.MessageResponse;
import com.auca.library.service.LibraryAnnouncementService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
public class LibraryAnnouncementController {

    @Autowired
    private LibraryAnnouncementService announcementService;

    // Create new announcement (admin only)
    @PostMapping("/api/admin/announcements")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<LibraryAnnouncementResponse> createAnnouncement(
            @Valid @RequestBody LibraryAnnouncementRequest request) {
        return ResponseEntity.ok(announcementService.createAnnouncement(request));
    }

    // Get all announcements (admin only)
    @GetMapping("/api/admin/announcements")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<LibraryAnnouncementResponse>> getAllAnnouncements() {
        return ResponseEntity.ok(announcementService.getAllAnnouncements());
    }

    // Get announcement by ID (admin only)
    @GetMapping("/api/admin/announcements/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<LibraryAnnouncementResponse> getAnnouncementById(@PathVariable Long id) {
        return ResponseEntity.ok(announcementService.getAnnouncementById(id));
    }

    // Update announcement (admin only)
    @PutMapping("/api/admin/announcements/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<LibraryAnnouncementResponse> updateAnnouncement(
            @PathVariable Long id,
            @Valid @RequestBody LibraryAnnouncementRequest request) {
        return ResponseEntity.ok(announcementService.updateAnnouncement(id, request));
    }

    // Delete announcement (admin only)
    @DeleteMapping("/api/admin/announcements/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MessageResponse> deleteAnnouncement(@PathVariable Long id) {
        return ResponseEntity.ok(announcementService.deleteAnnouncement(id));
    }

    // Public endpoint for getting active UI announcements (all users)
    @GetMapping("/api/announcements/active")
    public ResponseEntity<List<LibraryAnnouncementResponse>> getActiveAnnouncements() {
        return ResponseEntity.ok(announcementService.getActiveUIAnnouncements());
    }
}
