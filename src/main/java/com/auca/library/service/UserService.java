package com.auca.library.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.auca.library.dto.response.MessageResponse;
import com.auca.library.dto.response.UserResponse;
import com.auca.library.exception.ResourceNotFoundException;
import com.auca.library.model.User;
import com.auca.library.repository.UserRepository;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    // General user methods
    public List<UserResponse> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
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

    public UserResponse getUserById(Long id) {
        User user = findUserById(id);
        return mapToResponse(user);
    }

    public UserResponse getUserByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
        return mapToResponse(user);
    }

    public UserResponse getUserByIdentifier(String identifier) {
        User user = userRepository.findByIdentifier(identifier)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with identifier: " + identifier));
        return mapToResponse(user);
    }

    // Staff management methods
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

    // Fixed: Pass DayOfWeek instead of LocalDate
    public List<UserResponse> getActiveLibrariansForDay(LocalDate day) {
        DayOfWeek dayOfWeek = day.getDayOfWeek();
        return userRepository.findActiveLibrariansForDay(dayOfWeek).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public UserResponse getDefaultLibrarian() {
        User librarian = userRepository.findDefaultLibrarian()
                .orElseThrow(() -> new ResourceNotFoundException("No default librarian found"));
        return mapToResponse(librarian);
    }

    public UserResponse getActiveOrDefaultLibrarian(LocalDate day) {
        List<User> activeLibrarians = userRepository.findActiveLibrariansForDay(day.getDayOfWeek());
        if (!activeLibrarians.isEmpty()) {
            return mapToResponse(activeLibrarians.get(0));
        }

        return getDefaultLibrarian();
    }

    public List<UserResponse> getPendingProfessors() {
        return userRepository.findPendingProfessors().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<UserResponse> getApprovedProfessors() {
        return userRepository.findApprovedProfessors().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public UserResponse getEquipmentAdmin() {
        User admin = userRepository.findEquipmentAdmin()
                .orElseThrow(() -> new ResourceNotFoundException("No equipment admin found"));
        return mapToResponse(admin);
    }

    public UserResponse getHod() {
        User hod = userRepository.findHod()
                .orElseThrow(() -> new ResourceNotFoundException("No HOD found"));
        return mapToResponse(hod);
    }

    public List<UserResponse> getStaffWithDefaultPasswords() {
        return userRepository.findStaffWithDefaultPasswords().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public User getCurrentUser(Authentication authentication) {
        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Current user not found"));
    }

    // Update methods
    @Transactional
    public UserResponse updateUser(Long id, UserResponse updateRequest) {
        User user = findUserById(id);
        
        user.setFullName(updateRequest.getFullName());
        user.setPhone(updateRequest.getPhone());
        user.setLocation(updateRequest.getLocation());
        
        if (user.isLibrarian()) {
            user.setWorkingDays(updateRequest.getWorkingDays()); 
            user.setActiveThisWeek(updateRequest.isActiveThisWeek());
            
            // Fixed: Use isDefaultLibrarian instead of setDefaultLibrarian
            if (updateRequest.isDefaultLibrarian() && !user.isDefaultLibrarian()) { 
                userRepository.findDefaultLibrarianByLocation(user.getLocation()).ifPresent(existing -> {
                    existing.setDefaultLibrarian(false); 
                    userRepository.save(existing);
                });
                user.setDefaultLibrarian(true); 
            } else if (!updateRequest.isDefaultLibrarian()) { 
                user.setDefaultLibrarian(false); 
            }

            // Check active librarian limit for each working day
            if (updateRequest.isActiveThisWeek() && !user.isActiveThisWeek() && 
                updateRequest.getWorkingDays() != null && !updateRequest.getWorkingDays().isEmpty()) {
                
                for (DayOfWeek day : updateRequest.getWorkingDays()) {
                    // Count existing active librarians for this day and location
                    long activeCount = userRepository.countActiveLibrariansByDayAndLocation(day, user.getLocation());
                    
                    // If current user was previously active on this day, don't count them in the limit
                    if (user.isActiveThisWeek() && user.getWorkingDays() != null && user.getWorkingDays().contains(day)) {
                        activeCount--; // Subtract current user from count since they're being updated
                    }
                    
                    if (activeCount >= 2) {
                        throw new IllegalStateException(
                            "Only 2 librarians can be active on " + day + " at " + user.getLocation().getDisplayName()
                        );
                    }
                }
            }
        }

        return mapToResponse(userRepository.save(user));
    }

    @Transactional
    public MessageResponse deleteUser(Long id) {
        User user = findUserById(id);
        
        // Prevent deletion of default librarian without replacing
        if (user.isLibrarian() && user.isDefaultLibrarian()) {
            throw new IllegalStateException("Cannot delete default librarian. Please set another librarian as default first.");
        }
        
        userRepository.delete(user);
        return new MessageResponse("User deleted successfully.");
    }

    // Professor approval methods
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

    // Utility methods
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
            response.setActiveThisWeek(user.isActiveThisWeek()); // Fixed: Use isActiveThisWeek instead of isActiveToday
            response.setDefaultLibrarian(user.isDefaultLibrarian());
        }
        
        // Set professor-specific fields
        if (user.isProfessor()) {
            response.setProfessorApproved(user.isProfessorApproved());
            response.setProfessorApprovedAt(user.getProfessorApprovedAt());
        }
        
        return response;
    }
}