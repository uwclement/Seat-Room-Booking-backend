package com.auca.library.controller;

import com.auca.library.dto.response.RoomResponse;
import com.auca.library.model.RoomCategory;
import com.auca.library.service.AdminRoomService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/rooms")
@PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
public class PublicRoomController {

    @Autowired
    private AdminRoomService adminRoomService;

    @GetMapping
    public ResponseEntity<List<RoomResponse>> getAllAvailableRooms() {
        // Filter to only show available rooms for regular users
        List<RoomResponse> rooms = adminRoomService.filterRooms(null, true, null, null, null, null, null);
        return ResponseEntity.ok(rooms);
    }

    @GetMapping("/{id}")
    public ResponseEntity<RoomResponse> getRoomById(@PathVariable Long id) {
        RoomResponse room = adminRoomService.getRoomById(id);
        // You might want to filter sensitive information for non-admin users
        return ResponseEntity.ok(room);
    }

    @GetMapping("/category/{category}")
    public ResponseEntity<List<RoomResponse>> getAvailableRoomsByCategory(@PathVariable RoomCategory category) {
        List<RoomResponse> rooms = adminRoomService.filterRooms(category, true, null, null, null, null, null);
        return ResponseEntity.ok(rooms);
    }

    @GetMapping("/search")
    public ResponseEntity<List<RoomResponse>> searchAvailableRooms(@RequestParam String keyword) {
        // Filter search results to only show available rooms
        List<RoomResponse> allRooms = adminRoomService.searchRooms(keyword);
        List<RoomResponse> availableRooms = allRooms.stream()
                .filter(RoomResponse::isAvailable)
                .toList();
        return ResponseEntity.ok(availableRooms);
    }

    @GetMapping("/filter")
    public ResponseEntity<List<RoomResponse>> filterAvailableRooms(
            @RequestParam(required = false) RoomCategory category,
            @RequestParam(required = false) Integer minCapacity,
            @RequestParam(required = false) Integer maxCapacity,
            @RequestParam(required = false) String building,
            @RequestParam(required = false) String floor,
            @RequestParam(required = false) String department) {
        // Force available = true for public access
        List<RoomResponse> rooms = adminRoomService.filterRooms(
                category, true, minCapacity, maxCapacity, building, floor, department);
        return ResponseEntity.ok(rooms);
    }
}