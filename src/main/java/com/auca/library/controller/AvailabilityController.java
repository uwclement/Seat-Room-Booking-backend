package com.auca.library.controller;

import com.auca.library.dto.response.EquipmentAvailabilityResponse;
import com.auca.library.dto.response.LabClassAvailabilityResponse;
import com.auca.library.service.AvailabilityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/availability")
public class AvailabilityController {

    @Autowired
    private AvailabilityService availabilityService;

    @GetMapping("/equipment/{equipmentId}")
    @PreAuthorize("hasRole('PROFESSOR') or hasRole('USER') or hasRole('EQUIPMENT_ADMIN') or hasRole('LIBRARIAN')")
    public ResponseEntity<EquipmentAvailabilityResponse> getEquipmentAvailability(
            @PathVariable Long equipmentId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        
        EquipmentAvailabilityResponse availability = availabilityService.getEquipmentAvailability(
            equipmentId, startTime, endTime);
        return ResponseEntity.ok(availability);
    }

    @GetMapping("/lab-class/{labClassId}")
    @PreAuthorize("hasRole('PROFESSOR') or hasRole('EQUIPMENT_ADMIN') or hasRole('LIBRARIAN')")
    public ResponseEntity<LabClassAvailabilityResponse> getLabClassAvailability(
            @PathVariable Long labClassId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        
        LabClassAvailabilityResponse availability = availabilityService.getLabClassAvailability(
            labClassId, startTime, endTime);
        return ResponseEntity.ok(availability);
    }
}