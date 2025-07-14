package com.auca.library.controller;

import com.auca.library.dto.response.MessageResponse;
import com.auca.library.dto.response.UserResponse;
import com.auca.library.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/users")
public class UserController {
    @Autowired
    private UserService userService;

    // General user endpoints
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long id) {
        UserResponse user = userService.getUserById(id);
        return ResponseEntity.ok(user);
    }

    @GetMapping("/email/{email}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<UserResponse> getUserByEmail(@PathVariable String email) {
        UserResponse user = userService.getUserByEmail(email);
        return ResponseEntity.ok(user);
    }

    @GetMapping("/identifier/{identifier}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<UserResponse> getUserByIdentifier(@PathVariable String identifier) {
        UserResponse user = userService.getUserByIdentifier(identifier);
        return ResponseEntity.ok(user);
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        List<UserResponse> users = userService.getAllUsers();
        return ResponseEntity.ok(users);
    }

    @GetMapping("/students")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserResponse>> getAllStudents() {
        List<UserResponse> students = userService.getAllStudents();
        return ResponseEntity.ok(students);
    }

    @GetMapping("/staff")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserResponse>> getAllStaff() {
        List<UserResponse> staff = userService.getAllStaff();
        return ResponseEntity.ok(staff);
    }

    @GetMapping("/staff/default-passwords")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserResponse>> getStaffWithDefaultPasswords() {
        List<UserResponse> staff = userService.getStaffWithDefaultPasswords();
        return ResponseEntity.ok(staff);
    }

    // Role-specific endpoints
    @GetMapping("/admins")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserResponse>> getAllAdmins() {
        List<UserResponse> admins = userService.getAllAdmins();
        return ResponseEntity.ok(admins);
    }

    @GetMapping("/librarians")
    @PreAuthorize("hasRole('ADMIN') or hasRole('LIBRARIAN')")
    public ResponseEntity<List<UserResponse>> getAllLibrarians() {
        List<UserResponse> librarians = userService.getAllLibrarians();
        return ResponseEntity.ok(librarians);
    }

    @GetMapping("/librarians/active")
    @PreAuthorize("hasRole('ADMIN') or hasRole('LIBRARIAN')")
    public ResponseEntity<List<UserResponse>> getActiveLibrariansForDay(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate day) {
        List<UserResponse> librarians = userService.getActiveLibrariansForDay(day);
        return ResponseEntity.ok(librarians);
    }

    @GetMapping("/librarians/default")
    @PreAuthorize("hasRole('ADMIN') or hasRole('LIBRARIAN')")
    public ResponseEntity<UserResponse> getDefaultLibrarian() {
        UserResponse librarian = userService.getDefaultLibrarian();
        return ResponseEntity.ok(librarian);
    }

    @GetMapping("/librarians/active-or-default")
    @PreAuthorize("hasRole('ADMIN') or hasRole('LIBRARIAN')")
    public ResponseEntity<UserResponse> getActiveOrDefaultLibrarian(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate day) {
        UserResponse librarian = userService.getActiveOrDefaultLibrarian(day);
        return ResponseEntity.ok(librarian);
    }

    @GetMapping("/professors/pending")
    @PreAuthorize("hasRole('ADMIN') or hasRole('HOD')")
    public ResponseEntity<List<UserResponse>> getPendingProfessors() {
        List<UserResponse> professors = userService.getPendingProfessors();
        return ResponseEntity.ok(professors);
    }

    @GetMapping("/professors/approved")
    @PreAuthorize("hasRole('ADMIN') or hasRole('HOD')")
    public ResponseEntity<List<UserResponse>> getApprovedProfessors() {
        List<UserResponse> professors = userService.getApprovedProfessors();
        return ResponseEntity.ok(professors);
    }

    @GetMapping("/equipment-admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> getEquipmentAdmin() {
        UserResponse admin = userService.getEquipmentAdmin();
        return ResponseEntity.ok(admin);
    }

    @GetMapping("/hod")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> getHod() {
        UserResponse hod = userService.getHod();
        return ResponseEntity.ok(hod);
    }

    // Update and delete endpoints
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> updateUser(@PathVariable Long id, @RequestBody UserResponse updateRequest) {
        try {
            UserResponse updatedUser = userService.updateUser(id, updateRequest);
            return ResponseEntity.ok(updatedUser);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MessageResponse> deleteUser(@PathVariable Long id) {
        try {
            MessageResponse response = userService.deleteUser(id);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage()));
        }
    }

    @PostMapping("/professors/{professorId}/approve")
    @PreAuthorize("hasRole('HOD')")
    public ResponseEntity<MessageResponse> approveProfessor(
            @PathVariable Long professorId,
            @RequestParam Long hodId) {
        try {
            MessageResponse response = userService.approveProfessor(professorId, hodId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage()));
        }
    }
}