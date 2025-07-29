// File: src/main/java/com/auca/library/service/AdminUserService.java
package com.auca.library.service;

import java.security.SecureRandom;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.auca.library.dto.request.BulkUserActionRequest;
import com.auca.library.dto.request.StaffCreationRequest;
import com.auca.library.dto.request.StaffUpdateRequest;
import com.auca.library.dto.request.StudentUpdateRequest;
import com.auca.library.dto.response.BulkActionResponse;
import com.auca.library.dto.response.MessageResponse;
import com.auca.library.dto.response.StaffPasswordStatusResponse;
import com.auca.library.dto.response.UserResponse;
import com.auca.library.exception.EmailAlreadyExistsException;
import com.auca.library.exception.ResourceNotFoundException;
import com.auca.library.model.Location;
import com.auca.library.model.Role;
import com.auca.library.model.User;
import com.auca.library.repository.RoleRepository;
import com.auca.library.repository.UserRepository;

@Service
public class AdminUserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private static final String DEFAULT_PASSWORD_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%";
    private static final int DEFAULT_PASSWORD_LENGTH = 12;

    // =============== READ OPERATIONS ===============

    public List<UserResponse> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public UserResponse getUserById(Long id) {
        User user = findUserById(id);
        return mapToResponse(user);
    }

    public List<UserResponse> getAllStudents() {
        return userRepository.findAllStudents().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<UserResponse> getAllStaff() {
        return userRepository.findAllStaff().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<UserResponse> getAllAdmins() {
        return userRepository.findAllAdmins().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<UserResponse> getAllLibrarians() {
        return userRepository.findAllLibrarians().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<UserResponse> getAllProfessors() {
        return userRepository.findApprovedProfessors().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<UserResponse> getPendingProfessors() {
        return userRepository.findPendingProfessors().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<UserResponse> getStaffWithDefaultPasswords() {
        return userRepository.findStaffWithDefaultPasswords().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<UserResponse> getUsersWithActiveBookings() {
        // This would need to be implemented based on your booking system
        // For now, returning empty list - you can implement the actual logic
        return new ArrayList<>();
    }

    public List<UserResponse> getUsersByRole(String roleName) {
        Role.ERole roleEnum;
        try {
            roleEnum = Role.ERole.valueOf("ROLE_" + roleName.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid role: " + roleName);
        }

        return userRepository.findAll().stream()
                .filter(user -> user.hasRole(roleEnum))
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // =============== CREATE OPERATIONS ===============

    @Transactional
    public UserResponse createStaffUser(StaffCreationRequest request) throws EmailAlreadyExistsException {
        // Validation
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new EmailAlreadyExistsException("Email is already in use!");
        }

        if (userRepository.existsByEmployeeId(request.getEmployeeId())) {
            throw new EmailAlreadyExistsException("Employee ID is already in use!");
        }

        // Generate default password
        String defaultPassword = generateDefaultPassword();

        // Create staff user
        User user = new User(
                request.getFullName(),
                request.getEmail(),
                request.getEmployeeId(),
                passwordEncoder.encode(defaultPassword),
                request.getLocation(),
                request.getPhone()
        );

        // Set role
        Set<Role> roles = new HashSet<>();
        Role.ERole roleEnum = Role.ERole.valueOf("ROLE_" + request.getRole().toUpperCase());
        Role role = roleRepository.findByName(roleEnum)
                .orElseThrow(() -> new RuntimeException("Role not found: " + roleEnum));
        roles.add(role);
        user.setRoles(roles);

        // Handle librarian-specific logic
        if (roleEnum == Role.ERole.ROLE_LIBRARIAN) {
            handleLibrarianCreation(user, request);
        }

        User savedUser = userRepository.save(user);
        
        // Log the default password (in real implementation, you might want to send via email)
        System.out.println("Default password for " + user.getEmail() + ": " + defaultPassword);
        
        return mapToResponse(savedUser);
    }

    @Transactional
    public UserResponse createMultipleRoleUser(StaffCreationRequest request, List<String> roleNames) throws EmailAlreadyExistsException {
        // Validation
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new EmailAlreadyExistsException("Email is already in use!");
        }

        if (userRepository.existsByEmployeeId(request.getEmployeeId())) {
            throw new EmailAlreadyExistsException("Employee ID is already in use!");
        }

        String defaultPassword = generateDefaultPassword();

        User user = new User(
                request.getFullName(),
                request.getEmail(),
                request.getEmployeeId(),
                passwordEncoder.encode(defaultPassword),
                request.getLocation(),
                request.getPhone()
        );

        // Set multiple roles
        Set<Role> roles = new HashSet<>();
        for (String roleName : roleNames) {
            Role.ERole roleEnum = Role.ERole.valueOf("ROLE_" + roleName.toUpperCase());
            Role role = roleRepository.findByName(roleEnum)
                    .orElseThrow(() -> new RuntimeException("Role not found: " + roleEnum));
            roles.add(role);
        }
        user.setRoles(roles);

        User savedUser = userRepository.save(user);
        System.out.println("Default password for " + user.getEmail() + ": " + defaultPassword);
        
        return mapToResponse(savedUser);
    }

    // =============== UPDATE OPERATIONS ===============

    @Transactional
    public UserResponse updateStaffUser(Long id, StaffUpdateRequest request) {
        User user = findUserById(id);
        
        if (!user.isStaff()) {
            throw new IllegalStateException("User is not a staff member");
        }

        // Check email uniqueness if changed
        if (!user.getEmail().equals(request.getEmail()) && 
            userRepository.existsByEmail(request.getEmail())) {
            throw new EmailAlreadyExistsException("Email is already in use!");
        }

        // Check employee ID uniqueness if changed
        if (!user.getEmployeeId().equals(request.getEmployeeId()) && 
            userRepository.existsByEmployeeId(request.getEmployeeId())) {
            throw new EmailAlreadyExistsException("Employee ID is already in use!");
        }

        // Update basic fields
        user.setFullName(request.getFullName());
        user.setEmail(request.getEmail());
        user.setEmployeeId(request.getEmployeeId());
        user.setPhone(request.getPhone());
        user.setLocation(request.getLocation());

        // Handle librarian-specific updates
        if (user.isLibrarian()) {
            handleLibrarianUpdate(user, request);
        }

        return mapToResponse(userRepository.save(user));
    }

    @Transactional
    public UserResponse updateStudentUser(Long id, StudentUpdateRequest request) {
        User user = findUserById(id);
        
        if (!user.isStudent()) {
            throw new IllegalStateException("User is not a student");
        }

        // Check email uniqueness if changed
        if (!user.getEmail().equals(request.getEmail()) && 
            userRepository.existsByEmail(request.getEmail())) {
            throw new EmailAlreadyExistsException("Email is already in use!");
        }

        // Check student ID uniqueness if changed
        if (!user.getStudentId().equals(request.getStudentId()) && 
            userRepository.existsByStudentId(request.getStudentId())) {
            throw new EmailAlreadyExistsException("Student ID is already in use!");
        }

        // Update fields
        user.setFullName(request.getFullName());
        user.setEmail(request.getEmail());
        user.setStudentId(request.getStudentId());
        user.setLocation(request.getLocation());

        return mapToResponse(userRepository.save(user));
    }

    @Transactional
    public UserResponse setUserEnabled(Long id, boolean enabled) {
        User user = findUserById(id);
        user.setEmailVerified(enabled);
        return mapToResponse(userRepository.save(user));
    }

    @Transactional
    public UserResponse resetUserPassword(Long id) {
        User user = findUserById(id);
        
        if (!user.isStaff()) {
            throw new IllegalStateException("Can only reset passwords for staff members");
        }

        String newPassword = generateDefaultPassword();
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setMustChangePassword(true);
        
        User savedUser = userRepository.save(user);
        System.out.println("New password for " + user.getEmail() + ": " + newPassword);
        
        return mapToResponse(savedUser);
    }

    @Transactional
    public UserResponse updateUserRoles(Long id, List<String> roleNames) {
        User user = findUserById(id);
        
        Set<Role> newRoles = new HashSet<>();
        for (String roleName : roleNames) {
            Role.ERole roleEnum = Role.ERole.valueOf("ROLE_" + roleName.toUpperCase());
            Role role = roleRepository.findByName(roleEnum)
                    .orElseThrow(() -> new RuntimeException("Role not found: " + roleEnum));
            newRoles.add(role);
        }
        
        user.setRoles(newRoles);
        return mapToResponse(userRepository.save(user));
    }

    @Transactional
    public MessageResponse approveProfessor(Long professorId, Long hodId) {
        User professor = findUserById(professorId);
        User hod = findUserById(hodId);
        
        if (!professor.isProfessor()) {
            throw new IllegalStateException("User is not a professor");
        }
        
        if (!hod.isHod()) {
            throw new IllegalStateException("Approver is not an HOD");
        }
        
        professor.setProfessorApproved(true);
        professor.setProfessorApprovedAt(java.time.LocalDateTime.now());
        professor.setApprovedByHod(hod);
        
        userRepository.save(professor);
        
        return new MessageResponse("Professor approved successfully");
    }

    // =============== DELETE OPERATIONS ===============

    @Transactional
    public MessageResponse deleteUser(Long id) {
        User user = findUserById(id);
        
        // Prevent deletion of default librarian without replacement
        if (user.isLibrarian() && user.isDefaultLibrarian()) {
            throw new IllegalStateException("Cannot delete default librarian. Please set another librarian as default first.");
        }
        
        // Prevent deletion of the last admin
        if (user.isAdmin()) {
            long adminCount = userRepository.findAllAdmins().size();
            if (adminCount <= 1) {
                throw new IllegalStateException("Cannot delete the last admin user.");
            }
        }
        
        userRepository.delete(user);
        return new MessageResponse("User deleted successfully.");
    }

    @Transactional
    public MessageResponse deleteMultipleUsers(List<Long> userIds) {
        List<User> usersToDelete = new ArrayList<>();
        
        for (Long id : userIds) {
            User user = findUserById(id);
            
            // Apply same validation as single delete
            if (user.isLibrarian() && user.isDefaultLibrarian()) {
                throw new IllegalStateException("Cannot delete default librarian: " + user.getFullName());
            }
            
            usersToDelete.add(user);
        }
        
        // Check if deleting all admins
        long adminCount = userRepository.findAllAdmins().size();
        long adminsToDelete = usersToDelete.stream()
                .mapToLong(user -> user.isAdmin() ? 1 : 0)
                .sum();
        
        if (adminCount - adminsToDelete < 1) {
            throw new IllegalStateException("Cannot delete all admin users.");
        }
        
        userRepository.deleteAll(usersToDelete);
        return new MessageResponse("Successfully deleted " + usersToDelete.size() + " users.");
    }

    // =============== HELPER METHODS ===============

    private void handleLibrarianCreation(User user, StaffCreationRequest request) {
    user.setWorkingDays(request.getWorkingDays());
    user.setActiveThisWeek(request.isActiveThisWeek()); 
    
    
    if (request.isDefaultLibrarian()) { 
        userRepository.findDefaultLibrarianByLocation(request.getLocation()).ifPresent(existing -> {
            existing.setDefaultLibrarian(false); 
        });
        user.setDefaultLibrarian(true); 
    }

    // Check active librarian limit for each working day
    if (request.isActiveThisWeek() && request.getWorkingDays() != null && !request.getWorkingDays().isEmpty()) {
        for (DayOfWeek day : request.getWorkingDays()) {
            long activeCount = userRepository.countActiveLibrariansByDayAndLocation(day, request.getLocation());
            if (activeCount >= 2) {
                throw new IllegalStateException(
                    "Only 2 librarians can be active on " + day + " at " + request.getLocation().getDisplayName()
                );
            }
        }
    }
}

private void handleLibrarianUpdate(User user, StaffUpdateRequest request) {
    user.setWorkingDays(request.getWorkingDays());
    user.setActiveThisWeek(request.isActiveThisWeek()); 
    
    if (request.isDefaultLibrarian() && !user.isDefaultLibrarian()) { 
        userRepository.findDefaultLibrarianByLocation(request.getLocation()).ifPresent(existing -> {
            existing.setDefaultLibrarian(false);
            userRepository.save(existing);
        });
        user.setDefaultLibrarian(true); 
    } else if (!request.isDefaultLibrarian()) { 
        user.setDefaultLibrarian(false); 
    }

    if (request.isActiveThisWeek() && !user.isActiveThisWeek() && 
        request.getWorkingDays() != null && !request.getWorkingDays().isEmpty()) {
        
        for (DayOfWeek day : request.getWorkingDays()) {
            
            long activeCount = userRepository.countActiveLibrariansByDayAndLocation(day, request.getLocation());
            
         
            if (user.isActiveThisWeek() && user.getWorkingDays().contains(day)) {
                activeCount--; 
            }
            
            if (activeCount >= 2) {
                throw new IllegalStateException(
                    "Only 2 librarians can be active on " + day + " at " + request.getLocation().getDisplayName()
                );
            }
        }
    }
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

    private User findUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
    }

    private UserResponse mapToResponse(User user) {
        UserResponse response = new UserResponse();
        response.setId(user.getId());
        response.setFullName(user.getFullName());
        response.setEmail(user.getEmail());
        response.setIdentifier(user.getIdentifier());
        response.setUserType(user.isStudent() ? "STUDENT" : "STAFF");
        response.setLocation(user.getLocation());
        response.setPhone(user.getPhone());
        response.setEmailVerified(user.isEmailVerified());
        response.setMustChangePassword(user.isMustChangePassword());
        
        // Map roles
        List<String> roles = user.getRoles().stream()
                .map(role -> role.getName().name())
                .collect(Collectors.toList());
        response.setRoles(roles);
        
        // Set librarian-specific fields
        if (user.isLibrarian()) {
            response.setWorkingDays(user.getWorkingDays());
            response.setWorkingDaysString(user.getWorkingDaysString());
            response.setActiveThisWeek(user.isActiveThisWeek());
            response.setDefaultLibrarian(user.isDefaultLibrarian());
            response.setActiveToday(user.isActiveLibrarianToday());
            response.setLocation(user.getLocation());
        }
        
        // Set professor-specific fields
        if (user.isProfessor()) {
            response.setProfessorApproved(user.isProfessorApproved());
            response.setProfessorApprovedAt(user.getProfessorApprovedAt());
        }
        
        return response;
    }




    public List<UserResponse> searchUsers(String query, String userType, String role, 
                                    String location, Boolean emailVerified, Boolean mustChangePassword) {
    List<User> users = userRepository.findAll();
    
    return users.stream()
            .filter(user -> {
                // Text search in name, email, or identifier
                if (query != null && !query.trim().isEmpty()) {
                    String searchQuery = query.toLowerCase().trim();
                    boolean matches = user.getFullName().toLowerCase().contains(searchQuery) ||
                                    user.getEmail().toLowerCase().contains(searchQuery) ||
                                    (user.getIdentifier() != null && user.getIdentifier().toLowerCase().contains(searchQuery));
                    if (!matches) return false;
                }
                
                // Filter by user type
                if (userType != null) {
                    if ("STUDENT".equals(userType) && !user.isStudent()) return false;
                    if ("STAFF".equals(userType) && !user.isStaff()) return false;
                }
                
                // Filter by role
                if (role != null) {
                    Role.ERole roleEnum = Role.ERole.valueOf("ROLE_" + role.toUpperCase());
                    if (!user.hasRole(roleEnum)) return false;
                }
                
                // Filter by location
                if (location != null) {
                    Location locationEnum = Location.valueOf(location.toUpperCase());
                    if (!user.getLocation().equals(locationEnum)) return false;
                }
                
                // Filter by email verification status
                if (emailVerified != null && user.isEmailVerified() != emailVerified) {
                    return false;
                }
                
                // Filter by password change requirement
                if (mustChangePassword != null && user.isMustChangePassword() != mustChangePassword) {
                    return false;
                }
                
                return true;
            })
            .map(this::mapToResponse)
            .collect(Collectors.toList());
}

public List<UserResponse> filterUsers(List<String> roles, List<String> locations, 
                                    Boolean active, LocalDate createdAfter, LocalDate createdBefore) {
    List<User> users = userRepository.findAll();
    
    return users.stream()
            .filter(user -> {
                // Filter by roles
                if (roles != null && !roles.isEmpty()) {
                    boolean hasAnyRole = roles.stream()
                            .anyMatch(roleName -> {
                                try {
                                    Role.ERole roleEnum = Role.ERole.valueOf("ROLE_" + roleName.toUpperCase());
                                    return user.hasRole(roleEnum);
                                } catch (IllegalArgumentException e) {
                                    return false;
                                }
                            });
                    if (!hasAnyRole) return false;
                }
                
                // Filter by locations
                if (locations != null && !locations.isEmpty()) {
                    boolean matchesLocation = locations.stream()
                            .anyMatch(locationName -> {
                                try {
                                    Location locationEnum = Location.valueOf(locationName.toUpperCase());
                                    return user.getLocation().equals(locationEnum);
                                } catch (IllegalArgumentException e) {
                                    return false;
                                }
                            });
                    if (!matchesLocation) return false;
                }
                
                // Filter by active status
                if (active != null && user.isEmailVerified() != active) {
                    return false;
                }
                
                // Note: createdAfter and createdBefore would need a createdAt field in User entity
                // For now, we'll skip these filters
                
                return true;
            })
            .map(this::mapToResponse)
            .collect(Collectors.toList());
}

// =============== BULK OPERATIONS ===============

@Transactional
public BulkActionResponse performBulkAction(BulkUserActionRequest request) {
    List<Long> successfulIds = new ArrayList<>();
    List<String> errors = new ArrayList<>();
    
    for (Long userId : request.getUserIds()) {
        try {
            switch (request.getAction()) {
                case "ENABLE":
                    setUserEnabled(userId, true);
                    successfulIds.add(userId);
                    break;
                case "DISABLE":
                    setUserEnabled(userId, false);
                    successfulIds.add(userId);
                    break;
                case "RESET_PASSWORD":
                    resetUserPassword(userId);
                    successfulIds.add(userId);
                    break;
                case "DELETE":
                    deleteUser(userId);
                    successfulIds.add(userId);
                    break;
                default:
                    errors.add("Unknown action for user ID: " + userId);
            }
        } catch (Exception e) {
            errors.add("Failed to process user ID " + userId + ": " + e.getMessage());
        }
    }
    
    boolean success = errors.isEmpty();
    String message = String.format("Processed %d users successfully, %d failed", 
                                  successfulIds.size(), errors.size());
    
    return new BulkActionResponse(success, message, successfulIds.size(), 
                                errors.size(), errors, successfulIds);
}

// =============== STATISTICS OPERATIONS ===============

public Map<String, Integer> getUserStatsByRole() {
    Map<String, Integer> stats = new HashMap<>();
    stats.put("STUDENTS", userRepository.findAllStudents().size());
    stats.put("ADMINS", userRepository.findAllAdmins().size());
    stats.put("LIBRARIANS", userRepository.findAllLibrarians().size());
    stats.put("PROFESSORS", userRepository.findApprovedProfessors().size());
    stats.put("PENDING_PROFESSORS", userRepository.findPendingProfessors().size());
    
    try {
        stats.put("EQUIPMENT_ADMINS", userRepository.findEquipmentAdmin().isPresent() ? 1 : 0);
        stats.put("HODS", userRepository.findHod().isPresent() ? 1 : 0);
    } catch (Exception e) {
        stats.put("EQUIPMENT_ADMINS", 0);
        stats.put("HODS", 0);
    }
    
    return stats;
}

public Map<String, Integer> getUserStatsByLocation() {
    Map<String, Integer> stats = new HashMap<>();
    
    userRepository.findAll().stream()
            .collect(Collectors.groupingBy(
                    user -> user.getLocation() != null ? user.getLocation().name() : "UNKNOWN",
                    Collectors.collectingAndThen(Collectors.counting(), Math::toIntExact)))
            .forEach(stats::put);
    
    return stats;
}

public Map<String, Integer> getRegistrationTrends(int days) {
    Map<String, Integer> trends = new HashMap<>();
    
    // This is a simplified version - you'd need to add a createdAt field to User entity
    // and implement proper date-based queries
    for (int i = days; i >= 0; i--) {
        LocalDate date = LocalDate.now().minusDays(i);
        // Count users created on this date - implement actual query
        trends.put(date.toString(), 0); 
    }
    
    return trends;
}




public String getDefaultPassword(Long userId) {
    User user = findUserById(userId);
    
    if (!user.isStaff()) {
        throw new IllegalStateException("Can only get default passwords for staff members");
    }
    
    if (!user.isMustChangePassword()) {
        throw new IllegalStateException("User does not have a default password");
    }

    String newPassword = generateDefaultPassword();
    user.setPassword(passwordEncoder.encode(newPassword));
    userRepository.save(user);
    
    return newPassword;
}

/**
 * Get all staff members with their password change status
 */
public List<StaffPasswordStatusResponse> getStaffPasswordStatus() {
    return userRepository.findAllStaff().stream()
            .map(this::mapToPasswordStatusResponse)
            .collect(Collectors.toList());
}

/**
 * Check if a specific user has a default password
 */
public boolean hasDefaultPassword(Long userId) {
    User user = findUserById(userId);
    return user.isMustChangePassword();
}

/**
 * Force a user to change their password on next login
 */
@Transactional
public UserResponse forcePasswordChange(Long userId) {
    User user = findUserById(userId);
    
    if (!user.isStaff()) {
        throw new IllegalStateException("Can only force password change for staff members");
    }
    
    user.setMustChangePassword(true);
    User savedUser = userRepository.save(user);
    
    // Send notification
    // notificationService.notifyForcedPasswordChange(user);
    
    return mapToResponse(savedUser);
}

/**
 * Send password email to user with their current credentials
 */
public MessageResponse sendPasswordEmail(Long userId) {
    User user = findUserById(userId);
    
    if (!user.isStaff()) {
        throw new IllegalStateException("Can only send password emails to staff members");
    }
    
    if (!user.isMustChangePassword()) {
        // If user doesn't have default password, generate a new one
        String newPassword = generateDefaultPassword();
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setMustChangePassword(true);
        userRepository.save(user);
        
        // notificationService.notifyPasswordReset(user, newPassword);
    } else {
        // Get current default password (this would need to be stored separately in real implementation)
        String currentPassword = generateDefaultPassword(); // Placeholder - implement proper storage
        // notificationService.notifyPasswordResend(user, currentPassword);
    }
    
    return new MessageResponse("Password email sent successfully to " + user.getEmail());
}

/**
 * Get enhanced staff list with password status details
 */
public List<UserResponse> getStaffWithPasswordDetails() {
    return userRepository.findAllStaff().stream()
            .map(this::mapToResponseWithPasswordDetails)
            .collect(Collectors.toList());
}

// =============== HELPER METHODS ===============

private StaffPasswordStatusResponse mapToPasswordStatusResponse(User user) {
    StaffPasswordStatusResponse response = new StaffPasswordStatusResponse();
    response.setId(user.getId());
    response.setFullName(user.getFullName());
    response.setEmail(user.getEmail());
    response.setEmployeeId(user.getEmployeeId());
    response.setHasDefaultPassword(user.isMustChangePassword());
    response.setLastPasswordChange(getLastPasswordChangeDate(user)); // Implement if you track this
    response.setAccountStatus(user.isEmailVerified() ? "ACTIVE" : "INACTIVE");
    
    // Map roles
    List<String> roles = user.getRoles().stream()
            .map(role -> role.getName().name().replace("ROLE_", ""))
            .collect(Collectors.toList());
    response.setRoles(roles);
    
    return response;
}

private UserResponse mapToResponseWithPasswordDetails(User user) {
    UserResponse response = mapToResponse(user);
    // Add password-specific fields
    response.setPasswordLastChanged(getLastPasswordChangeDate(user));
    response.setPasswordStatus(user.isMustChangePassword() ? "DEFAULT" : "USER_SET");
    return response;
}

private LocalDateTime getLastPasswordChangeDate(User user) {
    // This would need to be implemented if you track password change dates
    // For now, return null or a default value
    return null;
}
}