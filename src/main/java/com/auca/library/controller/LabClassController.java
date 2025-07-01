package com.auca.library.controller;

import com.auca.library.dto.request.LabClassRequest;
import com.auca.library.dto.response.LabClassResponse;
import com.auca.library.dto.response.MessageResponse;
import com.auca.library.service.LabClassService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/equipment-admin/lab-classes")
@PreAuthorize("hasRole('EQUIPMENT_ADMIN')")
public class LabClassController {

    @Autowired
    private LabClassService labClassService;

    @GetMapping
    public ResponseEntity<List<LabClassResponse>> getAllLabClasses() {
        List<LabClassResponse> labClasses = labClassService.getAllLabClasses();
        return ResponseEntity.ok(labClasses);
    }

    @GetMapping("/available")
    public ResponseEntity<List<LabClassResponse>> getAvailableLabClasses() {
        List<LabClassResponse> labClasses = labClassService.getAvailableLabClasses();
        return ResponseEntity.ok(labClasses);
    }

    @GetMapping("/{id}")
    public ResponseEntity<LabClassResponse> getLabClassById(@PathVariable Long id) {
        LabClassResponse labClass = labClassService.getLabClassById(id);
        return ResponseEntity.ok(labClass);
    }

    @PostMapping
    public ResponseEntity<LabClassResponse> createLabClass(@Valid @RequestBody LabClassRequest request) {
        LabClassResponse labClass = labClassService.createLabClass(request);
        return ResponseEntity.ok(labClass);
    }

    @PutMapping("/{id}")
    public ResponseEntity<LabClassResponse> updateLabClass(@PathVariable Long id, @Valid @RequestBody LabClassRequest request) {
        LabClassResponse labClass = labClassService.updateLabClass(id, request);
        return ResponseEntity.ok(labClass);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<MessageResponse> deleteLabClass(@PathVariable Long id) {
        MessageResponse response = labClassService.deleteLabClass(id);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/toggle-availability")
    public ResponseEntity<LabClassResponse> toggleLabAvailability(@PathVariable Long id) {
        LabClassResponse labClass = labClassService.toggleLabAvailability(id);
        return ResponseEntity.ok(labClass);
    }

    @GetMapping("/search")
    public ResponseEntity<List<LabClassResponse>> searchLabClasses(@RequestParam String keyword) {
        List<LabClassResponse> labClasses = labClassService.searchLabClasses(keyword);
        return ResponseEntity.ok(labClasses);
    }
}