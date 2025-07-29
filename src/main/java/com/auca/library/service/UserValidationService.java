package com.auca.library.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.auca.library.model.Role;
import com.auca.library.model.User;
import com.auca.library.repository.UserRepository;

@Service
public class UserValidationService {

    @Autowired
    private UserRepository userRepository;

    public void validateUserCreation(String email, String studentId, String employeeId) {
        List<String> errors = new ArrayList<>();

        if (email != null && userRepository.existsByEmail(email)) {
            errors.add("Email is already in use");
        }

        if (studentId != null && userRepository.existsByStudentId(studentId)) {
            errors.add("Student ID is already in use");
        }

        if (employeeId != null && userRepository.existsByEmployeeId(employeeId)) {
            errors.add("Employee ID is already in use");
        }

        if (!errors.isEmpty()) {
            throw new IllegalArgumentException(String.join(", ", errors));
        }
    }

    public void validateUserUpdate(Long userId, String email, String studentId, String employeeId) {
        List<String> errors = new ArrayList<>();
        User currentUser = userRepository.findById(userId).orElse(null);

        if (currentUser == null) {
            throw new IllegalArgumentException("User not found");
        }

        // Check email uniqueness (if changed)
        if (email != null && !email.equals(currentUser.getEmail()) && 
            userRepository.existsByEmail(email)) {
            errors.add("Email is already in use");
        }

        // Check student ID uniqueness (if changed)
        if (studentId != null && !studentId.equals(currentUser.getStudentId()) && 
            userRepository.existsByStudentId(studentId)) {
            errors.add("Student ID is already in use");
        }

        // Check employee ID uniqueness (if changed)
        if (employeeId != null && !employeeId.equals(currentUser.getEmployeeId()) && 
            userRepository.existsByEmployeeId(employeeId)) {
            errors.add("Employee ID is already in use");
        }

        if (!errors.isEmpty()) {
            throw new IllegalArgumentException(String.join(", ", errors));
        }
    }

    // public void validateLibrarianConstraints(Long userId, boolean isActiveToday, 
    //                                        boolean isDefault, java.time.LocalDate workingDay) {
    //     // Check active librarian limit
    //     if (isActiveToday && workingDay != null) {
    //         long activeCount = userRepository.countActiveLibrariansForDay(workingDay);
            
    //         // If updating existing user, don't count current user
    //         if (userId != null) {
    //             User currentUser = userRepository.findById(userId).orElse(null);
    //             if (currentUser != null && currentUser.isActiveToday()) {
    //                 activeCount--;
    //             }
    //         }
            
    //         if (activeCount >= 2) {
    //             throw new IllegalStateException("Only 2 librarians can be active per day");
    //         }
    //     }

    //     // Handle default librarian logic
    //     if (isDefault) {
    //         User currentDefault = userRepository.findDefaultLibrarian().orElse(null);
    //         if (currentDefault != null && !currentDefault.getId().equals(userId)) {
    //             // Will need to update the existing default librarian
    //             // This is handled in the service layer
    //         }
    //     }
    // }

    public void validateUserDeletion(User user) {
        List<String> errors = new ArrayList<>();

        // Prevent deletion of default librarian without replacement
        if (user.isLibrarian() && user.isActiveToday()) {
            errors.add("Cannot delete default librarian. Please set another librarian as default first");
        }

        // Prevent deletion of the last admin
        if (user.isAdmin()) {
            long adminCount = userRepository.findAllAdmins().size();
            if (adminCount <= 1) {
                errors.add("Cannot delete the last admin user");
            }
        }

        // Add other business rules as needed
        // For example, check if user has active bookings, equipment requests, etc.

        if (!errors.isEmpty()) {
            throw new IllegalStateException(String.join(", ", errors));
        }
    }

    public void validateRoleAssignment(List<String> roleNames) {
        List<String> errors = new ArrayList<>();

        for (String roleName : roleNames) {
            try {
                Role.ERole.valueOf("ROLE_" + roleName.toUpperCase());
            } catch (IllegalArgumentException e) {
                errors.add("Invalid role: " + roleName);
            }
        }

        // Check for conflicting roles
        if (roleNames.contains("USER") && roleNames.stream().anyMatch(r -> !r.equals("USER"))) {
            errors.add("USER role cannot be combined with other roles");
        }

        if (!errors.isEmpty()) {
            throw new IllegalArgumentException(String.join(", ", errors));
        }
    }

    public void validateProfessorApproval(User professor, User hod) {
        List<String> errors = new ArrayList<>();

        if (!professor.isProfessor()) {
            errors.add("User is not a professor");
        }

        if (professor.isProfessorApproved()) {
            errors.add("Professor is already approved");
        }

        if (!hod.isHod()) {
            errors.add("Approver is not an HOD");
        }

        if (!errors.isEmpty()) {
            throw new IllegalStateException(String.join(", ", errors));
        }
    }
}
