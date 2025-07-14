package com.auca.library.controller;

import com.auca.library.dto.request.LoginRequest;
import com.auca.library.dto.request.PasswordChangeRequest;
import com.auca.library.dto.request.SignupRequest;
import com.auca.library.dto.request.StaffCreationRequest;
import com.auca.library.dto.response.JwtResponse;
import com.auca.library.dto.response.MessageResponse;
import com.auca.library.exception.EmailAlreadyExistsException;
import com.auca.library.service.AuthService;
import jakarta.mail.MessagingException;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import com.auca.library.security.services.UserDetailsImpl;
import org.springframework.web.bind.annotation.*;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    @Autowired
    private AuthService authService;

    @PostMapping("/signin")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
        try {
            JwtResponse response = authService.authenticateUser(loginRequest);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new MessageResponse("Login failed: " + e.getMessage()));
        }

        
    }

    @PostMapping("/signup")
    public ResponseEntity<?> registerStudent(@Valid @RequestBody SignupRequest signUpRequest) {
        try {
            MessageResponse response = authService.registerUser(signUpRequest);
            return ResponseEntity.ok(response);
        } catch (EmailAlreadyExistsException e) {
            return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage()));
        } catch (MessagingException e) {
            return ResponseEntity.internalServerError().body(new MessageResponse("Error sending verification email. Please try again."));
        }
    }

    @PostMapping("/create-staff")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> createStaffUser(@Valid @RequestBody StaffCreationRequest request) {
        try {
            MessageResponse response = authService.createStaffUser(request);
            return ResponseEntity.ok(response);
        } catch (EmailAlreadyExistsException e) {
            return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(new MessageResponse("Error creating staff user: " + e.getMessage()));
        }
    }

    @PostMapping("/change-password")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN') or hasRole('LIBRARIAN') or hasRole('PROFESSOR') or hasRole('HOD') or hasRole('EQUIPMENT_ADMIN')")
    public ResponseEntity<?> changePassword(
            @Valid @RequestBody PasswordChangeRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        try {
            MessageResponse response = authService.changePassword(userDetails.getId(), request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage()));
        }
    }

    @GetMapping("/verify")
    public ResponseEntity<?> verifyEmail(@RequestParam String token) {
        MessageResponse response = authService.verifyEmail(token);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/check-email")
    public ResponseEntity<?> checkEmailExists(@RequestParam String email) {
        boolean exists = authService.checkEmailExists(email);
        return ResponseEntity.ok(new MessageResponse(exists ? "Email exists" : "Email is available"));
    }

    @GetMapping("/check-student-id")
    public ResponseEntity<?> checkStudentIdExists(@RequestParam String studentId) {
        boolean exists = authService.checkStudentIdExists(studentId);
        return ResponseEntity.ok(new MessageResponse(exists ? "Student ID exists" : "Student ID is available"));
    }

    @GetMapping("/check-employee-id")
    public ResponseEntity<?> checkEmployeeIdExists(@RequestParam String employeeId) {
        boolean exists = authService.checkEmployeeIdExists(employeeId);
        return ResponseEntity.ok(new MessageResponse(exists ? "Employee ID exists" : "Employee ID is available"));
    }


    
}