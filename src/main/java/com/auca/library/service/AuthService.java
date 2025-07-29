package com.auca.library.service;

import java.security.SecureRandom;
import java.time.DayOfWeek;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.auca.library.dto.request.LoginRequest;
import com.auca.library.dto.request.PasswordChangeRequest;
import com.auca.library.dto.request.SignupRequest;
import com.auca.library.dto.request.StaffCreationRequest;
import com.auca.library.dto.response.JwtResponse;
import com.auca.library.dto.response.MessageResponse;
import com.auca.library.exception.EmailAlreadyExistsException;
import com.auca.library.model.Role;
import com.auca.library.model.User;
import com.auca.library.repository.RoleRepository;
import com.auca.library.repository.UserRepository;
import com.auca.library.security.jwt.JwtUtils;
import com.auca.library.security.services.UserDetailsImpl;

import jakarta.mail.MessagingException;

@Service
public class AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder encoder;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtUtils jwtUtils;
    
    @Autowired
    private EmailService emailService;

    private static final String DEFAULT_PASSWORD_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%";
    private static final int DEFAULT_PASSWORD_LENGTH = 12;

    // Student registration (self-registration)
    public MessageResponse registerUser(SignupRequest signUpRequest) throws EmailAlreadyExistsException, MessagingException {
        // Check if email exists
        if (userRepository.existsByEmail(signUpRequest.getEmail())) {
            throw new EmailAlreadyExistsException("Error: Email is already in use!");
        }

        // Check if student ID exists
        if (userRepository.existsByStudentId(signUpRequest.getStudentId())) {
            throw new EmailAlreadyExistsException("Error: Student ID is already in use!");
        }

        // Create new student account
        User user = new User(
                signUpRequest.getFullName(),
                signUpRequest.getEmail(),
                signUpRequest.getStudentId(),
                encoder.encode(signUpRequest.getPassword()),
                signUpRequest.getLocation()
        );

        Set<Role> roles = new HashSet<>();
        
        // By default, assign ROLE_USER for students
        Role userRole = roleRepository.findByName(Role.ERole.ROLE_USER)
                .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
        roles.add(userRole);
        
        // If admin role requested and exists in the request, add ROLE_ADMIN
        if (signUpRequest.getRoles() != null && 
            signUpRequest.getRoles().contains("admin")) {
            Role adminRole = roleRepository.findByName(Role.ERole.ROLE_ADMIN)
                    .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
            roles.add(adminRole);
        }

        user.setRoles(roles);
        
        // Generate verification token
        String token = UUID.randomUUID().toString();
        user.setVerificationToken(token);
        
        userRepository.save(user);
        
        // Send verification email
        emailService.sendVerificationEmail(user.getEmail(), token);

        return new MessageResponse("User registered successfully! Please check your email to verify your account.");
    }

    // Admin creates staff members
    @Transactional
    public MessageResponse createStaffUser(StaffCreationRequest request) throws EmailAlreadyExistsException {
        // Check if email exists
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new EmailAlreadyExistsException("Error: Email is already in use!");
        }

        // Check if employee ID exists
        if (userRepository.existsByEmployeeId(request.getEmployeeId())) {
            throw new EmailAlreadyExistsException("Error: Employee ID is already in use!");
        }

        // Generate default password
        String defaultPassword = generateDefaultPassword();

        User user = new User(
                request.getFullName(),
                request.getEmail(),
                request.getEmployeeId(),
                encoder.encode(defaultPassword),
                request.getLocation(),
                request.getPhone()
        );

        // Set role based on request
        Set<Role> roles = new HashSet<>();
        Role.ERole roleEnum = Role.ERole.valueOf("ROLE_" + request.getRole().toUpperCase());
        Role role = roleRepository.findByName(roleEnum)
                .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
        roles.add(role);
        user.setRoles(roles);

        // Set librarian based fields if applicable
        if (roleEnum == Role.ERole.ROLE_LIBRARIAN) {
            user.setWorkingDays(request.getWorkingDays());
            user.setActiveThisWeek(request.isActiveThisWeek());
            
            // Handle default librarian logic
            if (request.isDefaultLibrarian()) {
                // Remove default status from existing default librarian
                userRepository.findDefaultLibrarianByLocation(request.getLocation()).ifPresent(existing -> {
                    existing.setDefaultLibrarian(false);
                    userRepository.save(existing);
                });
                user.setDefaultLibrarian(true);
            }

            // Check active librarian limit
             if (request.isActiveThisWeek() && request.getWorkingDays() != null) {
                   for (DayOfWeek day : request.getWorkingDays()) {
                    long activeCount = userRepository.countActiveLibrariansByDayAndLocation(day, request.getLocation());
                   if (activeCount >= 2) {
                    throw new IllegalStateException("Only 2 librarians can be active on " + day + " at " + request.getLocation().getDisplayName());
                   }
                }
            
            }
        }

        userRepository.save(user);

        return new MessageResponse("Staff member created successfully. Default password: " + defaultPassword);
    }

    // Universal login method
    public JwtResponse authenticateUser(LoginRequest loginRequest) {
        // Find user by identifier (email, studentId, or employeeId)
        User user = userRepository.findByIdentifier(loginRequest.getIdentifier())
                .orElseThrow(() -> new RuntimeException("User not found with identifier: " + loginRequest.getIdentifier()));

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(user.getEmail(), loginRequest.getPassword()));

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = jwtUtils.generateJwtToken(authentication);
        
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        List<String> roles = userDetails.getAuthorities().stream()
                .map(item -> item.getAuthority())
                .collect(Collectors.toList());

        String userType = user.isStudent() ? "STUDENT" : "STAFF";
        String identifier = user.isStudent() ? user.getStudentId() : user.getEmployeeId();

        return new JwtResponse(
                jwt, 
                userDetails.getId(), 
                userDetails.getFullName(),
                userDetails.getEmail(),
                identifier,
                userDetails.getLocation().toString(),
                userType,
                userDetails.isEmailVerified(),
                user.isMustChangePassword(),
                roles);
    }

    // Password change method
    @Transactional
    public MessageResponse changePassword(Long userId, PasswordChangeRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
                
        // Verify current password
        if (!encoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new RuntimeException("Current password is incorrect");
        }

        // Update password
        user.setPassword(encoder.encode(request.getNewPassword()));
        user.setMustChangePassword(false);
        userRepository.save(user);

        return new MessageResponse("Password changed successfully");
    }

    public MessageResponse verifyEmail(String token) {
        Optional<User> userOpt = userRepository.findByVerificationToken(token);
        
        if (userOpt.isEmpty()) {
            return new MessageResponse("Invalid verification token");
        }
        
        User user = userOpt.get();
        user.setEmailVerified(true);
        user.setVerificationToken(null);
        userRepository.save(user);
        
        return new MessageResponse("Email verified successfully!");
    }
    
    public boolean checkEmailExists(String email) {
        return userRepository.existsByEmail(email);
    }

    public boolean checkStudentIdExists(String studentId) {
        return userRepository.existsByStudentId(studentId);
    }

    public boolean checkEmployeeIdExists(String employeeId) {
        return userRepository.existsByEmployeeId(employeeId);
    }

    private String generateDefaultPassword() {
        SecureRandom random = new SecureRandom();
        StringBuilder password = new StringBuilder();
        
        for (int i = 0; i < DEFAULT_PASSWORD_LENGTH; i++) {
            int randomIndex = random.nextInt(DEFAULT_PASSWORD_CHARS.length());
            password.append(DEFAULT_PASSWORD_CHARS.charAt(randomIndex));
        }
        
        return password.toString();
    }
}