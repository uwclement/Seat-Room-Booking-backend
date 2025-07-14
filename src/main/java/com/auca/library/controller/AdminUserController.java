package com.auca.library.controller;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
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

import com.auca.library.dto.request.BulkUserActionRequest;
import com.auca.library.dto.request.MultiRoleStaffCreationRequest;
import com.auca.library.dto.request.StaffCreationRequest;
import com.auca.library.dto.request.StaffUpdateRequest;
import com.auca.library.dto.request.StudentUpdateRequest;
import com.auca.library.dto.request.UserRoleUpdateRequest;
import com.auca.library.dto.response.AdminDashboardResponse;
import com.auca.library.dto.response.ApiResponse;
import com.auca.library.dto.response.BulkActionResponse;
import com.auca.library.dto.response.DefaultPasswordResponse;
import com.auca.library.dto.response.MessageResponse;
import com.auca.library.dto.response.StaffPasswordStatusResponse;
import com.auca.library.dto.response.UserResponse;
import com.auca.library.service.AdminDashboardService;
import com.auca.library.service.AdminUserService;

import jakarta.validation.Valid;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    @Autowired
    private AdminUserService adminUserService;

    @Autowired
    private AdminDashboardService adminDashboardService;

    // =============== DASHBOARD & OVERVIEW ===============

    @GetMapping("/dashboard")
    public ResponseEntity<AdminDashboardResponse> getDashboardStats() {
        return ResponseEntity.ok(adminDashboardService.getDashboardStats());
    }

    // =============== READ OPERATIONS ===============

    @GetMapping
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        return ResponseEntity.ok(adminUserService.getAllUsers());
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(adminUserService.getUserById(id));
    }

    @GetMapping("/students")
    public ResponseEntity<List<UserResponse>> getAllStudents() {
        return ResponseEntity.ok(adminUserService.getAllStudents());
    }

    @GetMapping("/staff")
    public ResponseEntity<List<UserResponse>> getAllStaff() {
        return ResponseEntity.ok(adminUserService.getAllStaff());
    }

    @GetMapping("/admins")
    public ResponseEntity<List<UserResponse>> getAllAdmins() {
        return ResponseEntity.ok(adminUserService.getAllAdmins());
    }

    @GetMapping("/librarians")
    public ResponseEntity<List<UserResponse>> getAllLibrarians() {
        return ResponseEntity.ok(adminUserService.getAllLibrarians());
    }

    @GetMapping("/professors")
    public ResponseEntity<List<UserResponse>> getAllProfessors() {
        return ResponseEntity.ok(adminUserService.getAllProfessors());
    }

    @GetMapping("/professors/pending")
    public ResponseEntity<List<UserResponse>> getPendingProfessors() {
        return ResponseEntity.ok(adminUserService.getPendingProfessors());
    }

    // @GetMapping("/staff/default-passwords")
    // public ResponseEntity<List<UserResponse>> getStaffWithDefaultPasswords() {
    //     return ResponseEntity.ok(adminUserService.getStaffWithDefaultPasswords());
    // }

    @GetMapping("/with-active-bookings")
    public ResponseEntity<List<UserResponse>> getUsersWithActiveBookings() {
        return ResponseEntity.ok(adminUserService.getUsersWithActiveBookings());
    }

    @GetMapping("/by-role/{roleName}")
    public ResponseEntity<List<UserResponse>> getUsersByRole(@PathVariable String roleName) {
        return ResponseEntity.ok(adminUserService.getUsersByRole(roleName));
    }

    @GetMapping("/librarians/active")
    public ResponseEntity<List<UserResponse>> getActiveLibrariansForDay(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate day) {
        // This would use your existing UserService method
        return ResponseEntity.ok(adminUserService.getAllLibrarians()); // Modify as needed
    }

    // =============== CREATE OPERATIONS ===============

    @PostMapping("/staff")
    public ResponseEntity<?> createStaffUser(@Valid @RequestBody StaffCreationRequest request) {
        try {
            UserResponse response = adminUserService.createStaffUser(request);
            return ResponseEntity.ok(ApiResponse.success("Staff user created successfully", response));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/staff/multi-role")
    public ResponseEntity<?> createMultiRoleStaff(@Valid @RequestBody MultiRoleStaffCreationRequest request) {
        try {
            UserResponse response = adminUserService.createMultipleRoleUser(
                convertToStaffCreationRequest(request), request.getRoles());
            return ResponseEntity.ok(ApiResponse.success("Multi-role staff user created successfully", response));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    // =============== UPDATE OPERATIONS ===============

    @PutMapping("/staff/{id}")
    public ResponseEntity<?> updateStaffUser(
            @PathVariable Long id, 
            @Valid @RequestBody StaffUpdateRequest request) {
        try {
            UserResponse response = adminUserService.updateStaffUser(id, request);
            return ResponseEntity.ok(ApiResponse.success("Staff user updated successfully", response));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PutMapping("/students/{id}")
    public ResponseEntity<?> updateStudentUser(
            @PathVariable Long id, 
            @Valid @RequestBody StudentUpdateRequest request) {
        try {
            UserResponse response = adminUserService.updateStudentUser(id, request);
            return ResponseEntity.ok(ApiResponse.success("Student user updated successfully", response));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<?> toggleUserStatus(
            @PathVariable Long id,
            @RequestParam boolean enabled) {
        try {
            UserResponse response = adminUserService.setUserEnabled(id, enabled);
            String message = enabled ? "User account enabled" : "User account disabled";
            return ResponseEntity.ok(ApiResponse.success(message, response));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PutMapping("/{id}/roles")
    public ResponseEntity<?> updateUserRoles(
            @PathVariable Long id,
            @Valid @RequestBody UserRoleUpdateRequest request) {
        try {
            UserResponse response = adminUserService.updateUserRoles(id, request.getRoles());
            return ResponseEntity.ok(ApiResponse.success("User roles updated successfully", response));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/{id}/reset-password")
    public ResponseEntity<?> resetUserPassword(@PathVariable Long id) {
        try {
            UserResponse response = adminUserService.resetUserPassword(id);
            return ResponseEntity.ok(ApiResponse.success("Password reset successfully", response));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/professors/{professorId}/approve")
    public ResponseEntity<?> approveProfessor(
            @PathVariable Long professorId,
            @RequestParam Long hodId) {
        try {
            MessageResponse response = adminUserService.approveProfessor(professorId, hodId);
            return ResponseEntity.ok(ApiResponse.success(response.getMessage(), null));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    // =============== DELETE OPERATIONS ===============

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {
        try {
            MessageResponse response = adminUserService.deleteUser(id);
            return ResponseEntity.ok(ApiResponse.success(response.getMessage(), null));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @DeleteMapping("/bulk")
    public ResponseEntity<?> deleteMultipleUsers(@Valid @RequestBody BulkUserActionRequest request) {
        try {
            if (!"DELETE".equals(request.getAction())) {
                return ResponseEntity.badRequest().body(ApiResponse.error("Invalid action for this endpoint"));
            }
            MessageResponse response = adminUserService.deleteMultipleUsers(request.getUserIds());
            return ResponseEntity.ok(ApiResponse.success(response.getMessage(), null));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    // =============== BULK OPERATIONS ===============

    @PostMapping("/bulk-actions")
    public ResponseEntity<?> performBulkAction(@Valid @RequestBody BulkUserActionRequest request) {
        try {
            BulkActionResponse response = adminUserService.performBulkAction(request);
            if (response.isSuccess()) {
                return ResponseEntity.ok(ApiResponse.success(response.getMessage(), response));
            } else {
                return ResponseEntity.badRequest().body(ApiResponse.error(response.getMessage()));
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/bulk-enable")
    public ResponseEntity<?> bulkEnableUsers(@RequestBody List<Long> userIds) {
        BulkUserActionRequest request = new BulkUserActionRequest();
        request.setUserIds(userIds);
        request.setAction("ENABLE");
        return performBulkAction(request);
    }

    @PostMapping("/bulk-disable")
    public ResponseEntity<?> bulkDisableUsers(@RequestBody List<Long> userIds) {
        BulkUserActionRequest request = new BulkUserActionRequest();
        request.setUserIds(userIds);
        request.setAction("DISABLE");
        return performBulkAction(request);
    }

    @PostMapping("/bulk-reset-passwords")
    public ResponseEntity<?> bulkResetPasswords(@RequestBody List<Long> userIds) {
        BulkUserActionRequest request = new BulkUserActionRequest();
        request.setUserIds(userIds);
        request.setAction("RESET_PASSWORD");
        return performBulkAction(request);
    }

    // =============== SEARCH AND FILTER OPERATIONS ===============

    @GetMapping("/search")
    public ResponseEntity<List<UserResponse>> searchUsers(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String userType,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) Boolean emailVerified,
            @RequestParam(required = false) Boolean mustChangePassword) {
        
        List<UserResponse> users = adminUserService.searchUsers(
                query, userType, role, location, emailVerified, mustChangePassword);
        return ResponseEntity.ok(users);
    }

    @GetMapping("/filter")
    public ResponseEntity<List<UserResponse>> filterUsers(
            @RequestParam(required = false) List<String> roles,
            @RequestParam(required = false) List<String> locations,
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) 
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate createdAfter,
            @RequestParam(required = false) 
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate createdBefore) {
        
        List<UserResponse> users = adminUserService.filterUsers(
                roles, locations, active, createdAfter, createdBefore);
        return ResponseEntity.ok(users);
    }

    // =============== EXPORT OPERATIONS ===============

    @GetMapping("/export/csv")
    public ResponseEntity<?> exportUsersToCSV(
            @RequestParam(required = false) String userType,
            @RequestParam(required = false) List<String> roles) {
        try {
            // This would return a CSV file - implement based on your needs
            return ResponseEntity.ok(ApiResponse.success("CSV export feature would be implemented here", null));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/export/excel")
    public ResponseEntity<?> exportUsersToExcel(
            @RequestParam(required = false) String userType,
            @RequestParam(required = false) List<String> roles) {
        try {
            // This would return an Excel file - implement based on your needs
            return ResponseEntity.ok(ApiResponse.success("Excel export feature would be implemented here", null));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    // =============== STATISTICS OPERATIONS ===============

    @GetMapping("/stats/by-role")
    public ResponseEntity<?> getUserStatsByRole() {
        try {
            var stats = adminUserService.getUserStatsByRole();
            return ResponseEntity.ok(ApiResponse.success("User statistics by role", stats));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/stats/by-location")
    public ResponseEntity<?> getUserStatsByLocation() {
        try {
            var stats = adminUserService.getUserStatsByLocation();
            return ResponseEntity.ok(ApiResponse.success("User statistics by location", stats));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/stats/registration-trends")
    public ResponseEntity<?> getRegistrationTrends(
            @RequestParam(required = false, defaultValue = "30") int days) {
        try {
            var trends = adminUserService.getRegistrationTrends(days);
            return ResponseEntity.ok(ApiResponse.success("Registration trends", trends));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    // =============== HELPER METHODS ===============

    private StaffCreationRequest convertToStaffCreationRequest(MultiRoleStaffCreationRequest request) {
        StaffCreationRequest staffRequest = new StaffCreationRequest();
        staffRequest.setFullName(request.getFullName());
        staffRequest.setEmail(request.getEmail());
        staffRequest.setEmployeeId(request.getEmployeeId());
        staffRequest.setPhone(request.getPhone());
        staffRequest.setLocation(request.getLocation());
        staffRequest.setRole(request.getRoles().get(0)); // Primary role
        staffRequest.setWorkingDay(request.getWorkingDay());
        staffRequest.setActiveToday(request.isActiveToday());
        staffRequest.setDefault(request.isDefault());
        return staffRequest;
    }


    @GetMapping("/{id}/default-password")
@PreAuthorize("hasRole('ADMIN')")
public ResponseEntity<?> getDefaultPassword(@PathVariable Long id) {
    try {
        String password = adminUserService.getDefaultPassword(id);
        UserResponse user = adminUserService.getUserById(id);
        
        DefaultPasswordResponse response = new DefaultPasswordResponse(
            password,
            "Default password retrieved successfully",
            true,
            user.getEmail(),
            user.getIdentifier()
        );
        
        return ResponseEntity.ok(ApiResponse.success("Default password retrieved", response));
    } catch (Exception e) {
        return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
    }
}

/**
 * Get all staff members with their password status
 */
@GetMapping("/staff/password-status")
@PreAuthorize("hasRole('ADMIN')")
public ResponseEntity<?> getStaffPasswordStatus() {
    try {
        List<StaffPasswordStatusResponse> staffStatus = adminUserService.getStaffPasswordStatus();
        return ResponseEntity.ok(ApiResponse.success("Staff password status retrieved", staffStatus));
    } catch (Exception e) {
        return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
    }
}

/**
 * Check if user has default password
 */
@GetMapping("/{id}/has-default-password")
@PreAuthorize("hasRole('ADMIN')")
public ResponseEntity<?> hasDefaultPassword(@PathVariable Long id) {
    try {
        boolean hasDefault = adminUserService.hasDefaultPassword(id);
        Map<String, Object> response = new HashMap<>();
        response.put("userId", id);
        response.put("hasDefaultPassword", hasDefault);
        response.put("message", hasDefault ? "User has default password" : "User has changed password");
        
        return ResponseEntity.ok(ApiResponse.success("Password status checked", response));
    } catch (Exception e) {
        return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
    }
}

/**
 * Force password change for user (admin only)
 */
@PostMapping("/{id}/force-password-change")
@PreAuthorize("hasRole('ADMIN')")
public ResponseEntity<?> forcePasswordChange(@PathVariable Long id) {
    try {
        UserResponse user = adminUserService.forcePasswordChange(id);
        return ResponseEntity.ok(ApiResponse.success("Password change forced successfully", user));
    } catch (Exception e) {
        return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
    }
}

/**
 * Send password email to user (admin only)
 */
@PostMapping("/{id}/send-password-email")
@PreAuthorize("hasRole('ADMIN')")
public ResponseEntity<?> sendPasswordEmail(@PathVariable Long id) {
    try {
        MessageResponse response = adminUserService.sendPasswordEmail(id);
        return ResponseEntity.ok(ApiResponse.success(response.getMessage(), null));
    } catch (Exception e) {
        return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
    }
}

/**
 * Get staff users who have default passwords (already exists, but enhanced)
 */
@GetMapping("/staff/default-passwords")
@PreAuthorize("hasRole('ADMIN')")
public ResponseEntity<?> getStaffWithDefaultPasswords() {
    try {
        List<UserResponse> staff = adminUserService.getStaffWithPasswordDetails();
        // Filter only those with default passwords
        List<UserResponse> defaultPasswordStaff = staff.stream()
                .filter(user -> user.isMustChangePassword())
                .collect(Collectors.toList());
                
        return ResponseEntity.ok(ApiResponse.success("Staff with default passwords retrieved", defaultPasswordStaff));
    } catch (Exception e) {
        return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
    }
}

/**
 * Bulk force password change for multiple users
 */
@PostMapping("/bulk-force-password-change")
@PreAuthorize("hasRole('ADMIN')")
public ResponseEntity<?> bulkForcePasswordChange(@RequestBody List<Long> userIds) {
    try {
        List<UserResponse> updatedUsers = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        
        for (Long userId : userIds) {
            try {
                UserResponse user = adminUserService.forcePasswordChange(userId);
                updatedUsers.add(user);
            } catch (Exception e) {
                errors.add("Failed to force password change for user " + userId + ": " + e.getMessage());
            }
        }
        
        BulkActionResponse response = new BulkActionResponse(
            errors.isEmpty(),
            String.format("Processed %d users, %d successful, %d failed", 
                         userIds.size(), updatedUsers.size(), errors.size()),
            updatedUsers.size(),
            errors.size(),
            errors,
            updatedUsers.stream().map(UserResponse::getId).collect(Collectors.toList())
        );
        
        return ResponseEntity.ok(ApiResponse.success("Bulk password change completed", response));
    } catch (Exception e) {
        return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
    }
}

/**
 * Bulk send password emails
 */
@PostMapping("/bulk-send-password-emails")
@PreAuthorize("hasRole('ADMIN')")
public ResponseEntity<?> bulkSendPasswordEmails(@RequestBody List<Long> userIds) {
    try {
        List<String> successEmails = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        
        for (Long userId : userIds) {
            try {
                MessageResponse response = adminUserService.sendPasswordEmail(userId);
                UserResponse user = adminUserService.getUserById(userId);
                successEmails.add(user.getEmail());
            } catch (Exception e) {
                errors.add("Failed to send email for user " + userId + ": " + e.getMessage());
            }
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("successCount", successEmails.size());
        response.put("failureCount", errors.size());
        response.put("successfulEmails", successEmails);
        response.put("errors", errors);
        
        return ResponseEntity.ok(ApiResponse.success("Bulk email sending completed", response));
    } catch (Exception e) {
        return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
    }
}

/**
 * Get password management statistics
 */
@GetMapping("/password-stats")
@PreAuthorize("hasRole('ADMIN')")
public ResponseEntity<?> getPasswordStats() {
    try {
        List<StaffPasswordStatusResponse> allStaff = adminUserService.getStaffPasswordStatus();
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalStaff", allStaff.size());
        stats.put("withDefaultPasswords", allStaff.stream().mapToInt(s -> s.isHasDefaultPassword() ? 1 : 0).sum());
        stats.put("withChangedPasswords", allStaff.stream().mapToInt(s -> !s.isHasDefaultPassword() ? 1 : 0).sum());
        stats.put("activeAccounts", allStaff.stream().mapToInt(s -> "ACTIVE".equals(s.getAccountStatus()) ? 1 : 0).sum());
        stats.put("inactiveAccounts", allStaff.stream().mapToInt(s -> "INACTIVE".equals(s.getAccountStatus()) ? 1 : 0).sum());
        
        return ResponseEntity.ok(ApiResponse.success("Password statistics retrieved", stats));
    } catch (Exception e) {
        return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
    }
}


}