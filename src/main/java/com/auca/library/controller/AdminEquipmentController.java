package com.auca.library.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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

import com.auca.library.dto.request.EquipmentRequest;
import com.auca.library.dto.response.EquipmentResponse;
import com.auca.library.dto.response.MessageResponse;
import com.auca.library.service.EquipmentService;

import jakarta.validation.Valid;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/equipment-admin/equipment")
@PreAuthorize("hasRole('EQUIPMENT_ADMIN') or hasRole ('ADMIN')")
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

    @GetMapping("/student-allowed")
    public ResponseEntity<List<EquipmentResponse>> getStudentAllowedEquipment() {
        List<EquipmentResponse> equipment = equipmentService.getStudentAllowedEquipment();
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