package com.auca.library.service;

import com.auca.library.model.*;
import com.auca.library.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class EquipmentValidationService {

    @Autowired
    private EquipmentRepository equipmentRepository;
    
    @Autowired
    private EquipmentRequestRepository equipmentRequestRepository;
    
    @Autowired
    private LabClassRepository labClassRepository;

    public void validateEquipmentRequest(User user, Equipment equipment, int requestedQuantity, 
                                       LocalDateTime startTime, LocalDateTime endTime) {
        
        // Basic validations
        if (!equipment.isAvailable()) {
            throw new IllegalArgumentException("Equipment is not available");
        }
        
        if (startTime.isAfter(endTime)) {
            throw new IllegalArgumentException("Start time must be before end time");
        }
        
        if (startTime.isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Cannot request equipment for past time");
        }
        
        // Student-specific validations
        if (hasRole(user, "ROLE_USER") && !equipment.isAllowedToStudents()) {
            throw new IllegalArgumentException("This equipment is not available for student requests");
        }
        
        // Quantity validation
        if (requestedQuantity <= 0) {
            throw new IllegalArgumentException("Requested quantity must be positive");
        }
        
        if (equipment.getQuantity() != null && requestedQuantity > equipment.getQuantity()) {
            throw new IllegalArgumentException("Requested quantity exceeds total available quantity");
        }
        
        // Check for conflicts
        List<EquipmentRequest> conflicts = equipmentRequestRepository.findConflictingRequests(
            equipment.getId(), startTime, endTime);
        
        int totalRequestedInTimeSlot = conflicts.stream()
                .mapToInt(EquipmentRequest::getRequestedQuantity)
                .sum() + requestedQuantity;
                
        if (equipment.getQuantity() != null && totalRequestedInTimeSlot > equipment.getQuantity()) {
            throw new IllegalArgumentException("Equipment not available for requested time slot");
        }
    }

    public void validateLabClassRequest(User user, LabClass labClass, LocalDateTime startTime, LocalDateTime endTime) {
        
        // Basic validations
        if (!labClass.isAvailable()) {
            throw new IllegalArgumentException("Lab class is not available");
        }
        
        if (startTime.isAfter(endTime)) {
            throw new IllegalArgumentException("Start time must be before end time");
        }
        
        if (startTime.isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Cannot request lab class for past time");
        }
        
        // Only professors can request lab classes
        if (!hasRole(user, "ROLE_PROFESSOR")) {
            throw new IllegalArgumentException("Only professors can request lab classes");
        }
        
        // Check lab availability
        if (!labClassRepository.isLabAvailable(labClass.getId(), startTime, endTime)) {
            throw new IllegalArgumentException("Lab class is not available for requested time slot");
        }
    }

    public void validateProfessorCourseAssociation(User professor, Course course) {
        if (!professor.isProfessorApproved()) {
            throw new IllegalArgumentException("Professor account must be approved first");
        }
        
        if (!professor.getApprovedCourses().contains(course)) {
            throw new IllegalArgumentException("You are not approved to teach this course");
        }
        
        if (!course.isActive()) {
            throw new IllegalArgumentException("Course is not active");
        }
    }

    private boolean hasRole(User user, String roleName) {
        return user.getRoles().stream()
                .anyMatch(role -> role.getName().name().equals(roleName));
    }
}