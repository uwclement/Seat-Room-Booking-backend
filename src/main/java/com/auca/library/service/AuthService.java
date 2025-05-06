package com.auca.library.service;

import com.auca.library.dto.request.LoginRequest;
import com.auca.library.dto.request.SignupRequest;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

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

    public MessageResponse registerUser(SignupRequest signUpRequest) throws EmailAlreadyExistsException, MessagingException {
        // Check if email exists
        if (userRepository.existsByEmail(signUpRequest.getEmail())) {
            throw new EmailAlreadyExistsException("Error: Email is already in use!");
        }

        // Check if student ID exists
        if (userRepository.existsByStudentId(signUpRequest.getStudentId())) {
            throw new EmailAlreadyExistsException("Error: Student ID is already in use!");
        }

        // Create new user's account
        User user = new User(
                signUpRequest.getFullName(),
                signUpRequest.getEmail(),
                signUpRequest.getStudentId(),
                encoder.encode(signUpRequest.getPassword())
        );

        Set<Role> roles = new HashSet<>();
        
        // By default, assign ROLE_USER
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

    public JwtResponse authenticateUser(LoginRequest loginRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getEmail(), loginRequest.getPassword()));

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = jwtUtils.generateJwtToken(authentication);
        
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        List<String> roles = userDetails.getAuthorities().stream()
                .map(item -> item.getAuthority())
                .collect(Collectors.toList());

        return new JwtResponse(
                jwt, 
                userDetails.getId(), 
                userDetails.getFullName(),
                userDetails.getEmail(),
                userDetails.getStudentId(),
                userDetails.isEmailVerified(),
                roles);
    }
    
    public MessageResponse verifyEmail(String token) {
        Optional<User> userOpt = userRepository.findByVerificationToken(token);
        
        if (userOpt.isEmpty()) {
            return new MessageResponse("Invalid verification token");
        }
        
        User user = userOpt.get();
        user.setEmailVerified(true);
        user.setVerificationToken(null); // Clear the token after use
        userRepository.save(user);
        
        return new MessageResponse("Email verified successfully!");
    }
    
    public boolean checkEmailExists(String email) {
        return userRepository.existsByEmail(email);
    }
}