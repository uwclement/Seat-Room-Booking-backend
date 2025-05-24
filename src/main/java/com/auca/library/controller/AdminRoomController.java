package com.auca.library.controller;

import com.auca.library.dto.request.*;
import com.auca.library.dto.response.*;
import com.auca.library.model.RoomCategory;
import com.auca.library.service.AdminRoomService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/admin/rooms")
@PreAuthorize("hasRole('ADMIN')")
public class AdminRoomController {

    @Autowired
    private AdminRoomService adminRoomService;

    // === Room CRUD Operations ===

    @GetMapping
    public ResponseEntity<List<RoomResponse>> getAllRooms() {
        List<RoomResponse> rooms = adminRoomService.getAllRooms();
        return ResponseEntity.ok(rooms);
    }

    @GetMapping("/{id}")
    public ResponseEntity<RoomResponse> getRoomById(@PathVariable Long id) {
        RoomResponse room = adminRoomService.getRoomById(id);
        return ResponseEntity.ok(room);
    }

    @GetMapping("/category/{category}")
    public ResponseEntity<List<RoomResponse>> getRoomsByCategory(@PathVariable RoomCategory category) {
        List<RoomResponse> rooms = adminRoomService.getRoomsByCategory(category);
        return ResponseEntity.ok(rooms);
    }

    @PostMapping
    public ResponseEntity<RoomResponse> createRoom(@Valid @RequestBody RoomCreateRequest request) {
        RoomResponse room = adminRoomService.createRoom(request);
        return ResponseEntity.ok(room);
    }

    @PutMapping("/{id}")
    public ResponseEntity<RoomResponse> updateRoom(@PathVariable Long id, @Valid @RequestBody RoomUpdateRequest request) {
        RoomResponse room = adminRoomService.updateRoom(id, request);
        return ResponseEntity.ok(room);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<MessageResponse> deleteRoom(@PathVariable Long id) {
        MessageResponse response = adminRoomService.deleteRoom(id);
        return ResponseEntity.ok(response);
    }

    // === Room Status Management ===

    @PostMapping("/{id}/toggle-availability")
    public ResponseEntity<RoomResponse> toggleRoomAvailability(@PathVariable Long id) {
        RoomResponse room = adminRoomService.toggleRoomAvailability(id);
        return ResponseEntity.ok(room);
    }

    @PostMapping("/{id}/maintenance")
    public ResponseEntity<RoomResponse> setMaintenanceWindow(
            @PathVariable Long id,
            @RequestParam LocalDateTime startTime,
            @RequestParam LocalDateTime endTime,
            @RequestParam(required = false) String notes) {
        RoomResponse room = adminRoomService.setMaintenanceWindow(id, startTime, endTime, notes);
        return ResponseEntity.ok(room);
    }

    @DeleteMapping("/{id}/maintenance")
    public ResponseEntity<RoomResponse> clearMaintenanceWindow(@PathVariable Long id) {
        RoomResponse room = adminRoomService.clearMaintenanceWindow(id);
        return ResponseEntity.ok(room);
    }

    // === Equipment Management ===

    @PostMapping("/{id}/equipment")
    public ResponseEntity<RoomResponse> addEquipmentToRoom(
            @PathVariable Long id,
            @RequestBody Set<Long> equipmentIds) {
        RoomResponse room = adminRoomService.addEquipmentToRoom(id, equipmentIds);
        return ResponseEntity.ok(room);
    }

    @DeleteMapping("/{id}/equipment")
    public ResponseEntity<RoomResponse> removeEquipmentFromRoom(
            @PathVariable Long id,
            @RequestBody Set<Long> equipmentIds) {
        RoomResponse room = adminRoomService.removeEquipmentFromRoom(id, equipmentIds);
        return ResponseEntity.ok(room);
    }

    // === Bulk Operations ===

    @PostMapping("/bulk-operation")
    public ResponseEntity<MessageResponse> performBulkOperation(@Valid @RequestBody BulkRoomOperationRequest request) {
        MessageResponse response = adminRoomService.performBulkOperation(request);
        return ResponseEntity.ok(response);
    }

    // === Search and Filter ===

    @GetMapping("/search")
    public ResponseEntity<List<RoomResponse>> searchRooms(@RequestParam String keyword) {
        List<RoomResponse> rooms = adminRoomService.searchRooms(keyword);
        return ResponseEntity.ok(rooms);
    }

    @GetMapping("/filter")
    public ResponseEntity<List<RoomResponse>> filterRooms(
            @RequestParam(required = false) RoomCategory category,
            @RequestParam(required = false) Boolean available,
            @RequestParam(required = false) Integer minCapacity,
            @RequestParam(required = false) Integer maxCapacity,
            @RequestParam(required = false) String building,
            @RequestParam(required = false) String floor,
            @RequestParam(required = false) String department) {
        List<RoomResponse> rooms = adminRoomService.filterRooms(
                category, available, minCapacity, maxCapacity, building, floor, department);
        return ResponseEntity.ok(rooms);
    }

    // === Room Templates ===

    @GetMapping("/templates")
    public ResponseEntity<List<RoomTemplateResponse>> getAllTemplates() {
        List<RoomTemplateResponse> templates = adminRoomService.getAllTemplates();
        return ResponseEntity.ok(templates);
    }

    @GetMapping("/templates/{id}")
    public ResponseEntity<RoomTemplateResponse> getTemplateById(@PathVariable Long id) {
        RoomTemplateResponse template = adminRoomService.getTemplateById(id);
        return ResponseEntity.ok(template);
    }

    @PostMapping("/templates")
    public ResponseEntity<RoomTemplateResponse> createTemplate(@Valid @RequestBody RoomTemplateRequest request) {
        RoomTemplateResponse template = adminRoomService.createTemplate(request);
        return ResponseEntity.ok(template);
    }

    @PostMapping("/templates/{templateId}/create-room")
    public ResponseEntity<RoomResponse> createRoomFromTemplate(
            @PathVariable Long templateId,
            @RequestParam String roomNumber,
            @RequestParam String name) {
        RoomResponse room = adminRoomService.createRoomFromTemplate(templateId, roomNumber, name);
        return ResponseEntity.ok(room);
    }

    @DeleteMapping("/templates/{id}")
    public ResponseEntity<MessageResponse> deleteTemplate(@PathVariable Long id) {
        MessageResponse response = adminRoomService.deleteTemplate(id);
        return ResponseEntity.ok(response);
    }

    // === Monitoring and Statistics ===

    @GetMapping("/maintenance")
    public ResponseEntity<List<RoomResponse>> getRoomsUnderMaintenance() {
        List<RoomResponse> rooms = adminRoomService.getRoomsUnderMaintenance();
        return ResponseEntity.ok(rooms);
    }

    @GetMapping("/recently-updated")
    public ResponseEntity<List<RoomResponse>> getRecentlyUpdatedRooms(@RequestParam(defaultValue = "24") int hours) {
        List<RoomResponse> rooms = adminRoomService.getRecentlyUpdatedRooms(hours);
        return ResponseEntity.ok(rooms);
    }
}