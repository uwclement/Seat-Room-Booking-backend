package com.auca.library.dto.response;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.auca.library.model.Location;

import lombok.Data;

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
    private Set<DayOfWeek> workingDays;
    private String workingDaysString;
    private boolean activeThisWeek;
    private boolean isDefaultLibrarian;
    private boolean isActiveToday;
    
    // Professor based fields
    private boolean professorApproved;
    private LocalDateTime professorApprovedAt;
    private List<CourseResponse> assignedCourses = new ArrayList<>();


    // Password management fields
    private LocalDateTime passwordLastChanged;
    private String passwordStatus; // DEFAULT, USER_SET, EXPIRED
    private boolean passwordEmailSent;
    private LocalDateTime passwordEmailSentAt;
}