package com.auca.library.controller;

import com.auca.library.dto.request.EquipmentRequest;
import com.auca.library.dto.response.EquipmentResponse;
import com.auca.library.dto.response.MessageResponse;
import com.auca.library.service.EquipmentService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/admin/equipment")
@PreAuthorize("hasRole('ADMIN')")
public class AdminEquipmentController {

    @Autowired
    private EquipmentService equipmentService;

    @GetMapping
    public ResponseEntity<List<EquipmentResponse>> getAllEquipment() {
        List<EquipmentResponse> equipment = equipmentService.getAllEquipment();
        return ResponseEntity.ok(equipment);
    }

    @GetMapping("/available")
    public ResponseEntity<List<EquipmentResponse>> getAvailableEquipment() {
        List<EquipmentResponse> equipment = equipmentService.getAvailableEquipment();
        return ResponseEntity.ok(equipment);
    }

    @GetMapping("/{id}")
    public ResponseEntity<EquipmentResponse> getEquipmentById(@PathVariable Long id) {
        EquipmentResponse equipment = equipmentService.getEquipmentById(id);
        return ResponseEntity.ok(equipment);
    }

    @PostMapping
    public ResponseEntity<EquipmentResponse> createEquipment(@Valid @RequestBody EquipmentRequest request) {
        EquipmentResponse equipment = equipmentService.createEquipment(request);
        return ResponseEntity.ok(equipment);
    }

    @PutMapping("/{id}")
    public ResponseEntity<EquipmentResponse> updateEquipment(@PathVariable Long id, @Valid @RequestBody EquipmentRequest request) {
        EquipmentResponse equipment = equipmentService.updateEquipment(id, request);
        return ResponseEntity.ok(equipment);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<MessageResponse> deleteEquipment(@PathVariable Long id) {
        MessageResponse response = equipmentService.deleteEquipment(id);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/toggle-availability")
    public ResponseEntity<EquipmentResponse> toggleEquipmentAvailability(@PathVariable Long id) {
        EquipmentResponse equipment = equipmentService.toggleEquipmentAvailability(id);
        return ResponseEntity.ok(equipment);
    }

    @GetMapping("/search")
    public ResponseEntity<List<EquipmentResponse>> searchEquipment(@RequestParam String keyword) {
        List<EquipmentResponse> equipment = equipmentService.searchEquipment(keyword);
        return ResponseEntity.ok(equipment);
    }
}

