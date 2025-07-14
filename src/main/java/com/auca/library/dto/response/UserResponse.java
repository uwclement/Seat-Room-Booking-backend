package com.auca.library.dto.response;

import com.auca.library.model.Location;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class UserResponse {
    private Long id;
    private String fullName;
    private String email;
    private String identifier; 
    private String userType; 
    private Location location;
    private String phone;
    private List<String> roles;
    private boolean emailVerified;
    private boolean mustChangePassword;
    
    // Librarian based fields
    private LocalDate workingDay;
    private boolean activeToday;
    private boolean isDefault;
    
    // Professor based fields
    private boolean professorApproved;
    private LocalDateTime professorApprovedAt;


    // Password management fields
    private LocalDateTime passwordLastChanged;
    private String passwordStatus; // DEFAULT, USER_SET, EXPIRED
    private boolean passwordEmailSent;
    private LocalDateTime passwordEmailSentAt;
}