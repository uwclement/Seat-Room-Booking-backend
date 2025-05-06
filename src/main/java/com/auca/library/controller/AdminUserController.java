package com.auca.library.controller;

import com.auca.library.dto.response.MessageResponse;
import com.auca.library.dto.response.UserResponse;
import com.auca.library.service.AdminUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    @Autowired
    private AdminUserService adminUserService;

    // Get all users
    @GetMapping
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        return ResponseEntity.ok(adminUserService.getAllUsers());
    }

    // Get user by ID
    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(adminUserService.getUserById(id));
    }

    // Enable/disable user account
    @PutMapping("/{id}/status")
    public ResponseEntity<UserResponse> toggleUserStatus(
            @PathVariable Long id,
            @RequestParam boolean enabled) {
        return ResponseEntity.ok(adminUserService.setUserEnabled(id, enabled));
    }

    // Get users with active bookings
    @GetMapping("/with-active-bookings")
    public ResponseEntity<List<UserResponse>> getUsersWithActiveBookings() {
        return ResponseEntity.ok(adminUserService.getUsersWithActiveBookings());
    }
}