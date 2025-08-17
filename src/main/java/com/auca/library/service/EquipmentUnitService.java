package com.auca.library.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.auca.library.dto.request.EquipmentAssignmentRequest;
import com.auca.library.dto.request.EquipmentUnitRequest;
import com.auca.library.dto.response.EquipmentAssignmentResponse;
import com.auca.library.dto.response.EquipmentUnitResponse;
import com.auca.library.dto.response.MessageResponse;
import com.auca.library.exception.ResourceNotFoundException;
import com.auca.library.model.Equipment;
import com.auca.library.model.EquipmentAssignment;
import com.auca.library.model.EquipmentUnit;
import com.auca.library.model.Location;
import com.auca.library.model.Room;
import com.auca.library.model.User;
import com.auca.library.repository.EquipmentAssignmentRepository;
import com.auca.library.repository.EquipmentRepository;
import com.auca.library.repository.EquipmentUnitRepository;
import com.auca.library.repository.RoomRepository;
import com.auca.library.repository.UserRepository;

@Service
public class EquipmentUnitService {

    @Autowired
    private EquipmentUnitRepository equipmentUnitRepository;
    
    @Autowired
    private EquipmentAssignmentRepository assignmentRepository;
    
    @Autowired
    private EquipmentRepository equipmentRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private RoomRepository roomRepository;

    // Create individual equipment unit
    @Transactional
    public EquipmentUnitResponse createEquipmentUnit(EquipmentUnitRequest request, User admin) {
        Equipment equipment = findEquipmentById(request.getEquipmentId());
        
        // Validate location access
        validateLocationAccess(equipment.getLocation(), admin);
        
        // Check serial number uniqueness
        if (equipmentUnitRepository.existsBySerialNumber(request.getSerialNumber())) {
            throw new IllegalArgumentException("Serial number already exists: " + request.getSerialNumber());
        }
        
        EquipmentUnit unit = new EquipmentUnit(equipment, request.getSerialNumber());
        unit.setCondition(request.getCondition() != null ? request.getCondition() : "GOOD");
        unit.setPurchaseDate(request.getPurchaseDate());
        unit.setWarrantyExpiry(request.getWarrantyExpiry());
        unit.setNotes(request.getNotes());
        
        unit = equipmentUnitRepository.save(unit);
        return mapToResponse(unit);
    }

    // Get equipment units by location
    public List<EquipmentUnitResponse> getEquipmentUnitsByLocation(Location location, User admin) {
        validateLocationAccess(location, admin);
        
        return equipmentUnitRepository.findByEquipmentLocation(location).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // Get available units for equipment request
    public List<EquipmentUnitResponse> getAvailableUnitsForEquipment(Long equipmentId, User admin) {
        Equipment equipment = findEquipmentById(equipmentId);
        validateLocationAccess(equipment.getLocation(), admin);
        
        return equipmentUnitRepository.findAvailableByEquipment(equipment).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // Assign equipment unit
    @Transactional
public EquipmentAssignmentResponse assignEquipmentUnit(EquipmentAssignmentRequest request, User admin) {
    EquipmentUnit unit = findEquipmentUnitById(request.getEquipmentUnitId());
    validateLocationAccess(unit.getLocation(), admin);
    
    // Check if unit is available
    if (!unit.isAvailable()) {
        throw new IllegalStateException("Equipment unit is not available for assignment");
    }
    
    // Create assignment with simplified approach
    EquipmentAssignment assignment = new EquipmentAssignment();
    assignment.setEquipmentUnit(unit);
    assignment.setAssignmentType(EquipmentAssignment.AssignmentType.valueOf(request.getAssignmentType()));
    assignment.setAssignedToName(request.getAssignedToName()); // Simple name field
    assignment.setAssignmentPeriod(EquipmentAssignment.AssignmentPeriod.valueOf(request.getAssignmentPeriod()));
    assignment.setStartDate(request.getStartDate() != null ? request.getStartDate() : LocalDateTime.now());
    assignment.setEndDate(request.getEndDate());
    assignment.setAssignedBy(admin);
    assignment.setAssignedAt(LocalDateTime.now());
    assignment.setStatus(EquipmentAssignment.AssignmentStatus.ACTIVE);
    
    assignment = assignmentRepository.save(assignment);
    
    // Update unit status
    unit.setStatus(EquipmentUnit.UnitStatus.ASSIGNED);
    equipmentUnitRepository.save(unit);
    
    return mapAssignmentToResponse(assignment);
}

    // Remove assignment (return equipment)
    @Transactional
    public MessageResponse removeAssignment(Long assignmentId, String returnReason, User admin) {
        EquipmentAssignment assignment = findAssignmentById(assignmentId);
        validateLocationAccess(assignment.getEquipmentUnit().getLocation(), admin);
        
        if (!assignment.isActive()) {
            throw new IllegalStateException("Assignment is not active");
        }
        
        // Update assignment
        assignment.setStatus(EquipmentAssignment.AssignmentStatus.RETURNED);
        assignment.setReturnedAt(LocalDateTime.now());
        assignment.setReturnedBy(admin);
        assignment.setReturnReason(returnReason);
        assignmentRepository.save(assignment);
        
        // Update unit status based on return reason
        EquipmentUnit unit = assignment.getEquipmentUnit();
        if (returnReason.toLowerCase().contains("maintenance")) {
            unit.setStatus(EquipmentUnit.UnitStatus.MAINTENANCE);
        } else if (returnReason.toLowerCase().contains("damage")) {
            unit.setStatus(EquipmentUnit.UnitStatus.DAMAGED);
        } else {
            unit.setStatus(EquipmentUnit.UnitStatus.AVAILABLE);
        }
        equipmentUnitRepository.save(unit);
        
        return new MessageResponse("Equipment assignment removed successfully. Reason: " + returnReason);
    }

    // Get assignments by location
    public List<EquipmentAssignmentResponse> getAssignmentsByLocation(Location location, User admin) {
        validateLocationAccess(location, admin);
        
        return assignmentRepository.findByLocationAndStatus(location, EquipmentAssignment.AssignmentStatus.ACTIVE)
                .stream()
                .map(this::mapAssignmentToResponse)
                .collect(Collectors.toList());
    }

    // Update equipment unit for request approval
    @Transactional
    public void assignUnitToRequest(Long equipmentRequestId, Long equipmentUnitId, User admin) {
        EquipmentUnit unit = findEquipmentUnitById(equipmentUnitId);
        validateLocationAccess(unit.getLocation(), admin);
        
        if (!unit.isAvailable()) {
            throw new IllegalStateException("Equipment unit is not available");
        }
        
        // Create request assignment
        EquipmentAssignment assignment = new EquipmentAssignment(unit, 
                EquipmentAssignment.AssignmentType.REQUEST_ASSIGNMENT, admin);
        assignment.setEquipmentRequest(findEquipmentRequestById(equipmentRequestId));
        assignmentRepository.save(assignment);
        
        // Update unit status
        unit.setStatus(EquipmentUnit.UnitStatus.IN_REQUEST);
        equipmentUnitRepository.save(unit);
    }

    // Helper methods
    private void validateLocationAccess(Location location, User admin) {
        if (!admin.getRoles().stream().anyMatch(role -> role.getName().name().equals("ROLE_ADMIN"))) {
            if (!admin.getLocation().equals(location)) {
                throw new SecurityException("Access denied: Cannot access equipment from different location");
            }
        }
    }

    private Equipment findEquipmentById(Long id) {
        return equipmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Equipment not found: " + id));
    }

    private EquipmentUnit findEquipmentUnitById(Long id) {
        return equipmentUnitRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Equipment unit not found: " + id));
    }

    private EquipmentAssignment findAssignmentById(Long id) {
        return assignmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Assignment not found: " + id));
    }

    private User findUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));
    }

    private Room findRoomById(Long id) {
        return roomRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found: " + id));
    }

    private com.auca.library.model.EquipmentRequest findEquipmentRequestById(Long id) {
        // This should use your existing EquipmentRequestRepository
        throw new UnsupportedOperationException("Implement with your existing EquipmentRequestRepository");
    }

    private EquipmentUnitResponse mapToResponse(EquipmentUnit unit) {
        EquipmentUnitResponse response = new EquipmentUnitResponse();
        response.setId(unit.getId());
        response.setSerialNumber(unit.getSerialNumber());
        response.setStatus(unit.getStatus());
        response.setCondition(unit.getCondition());
        response.setEquipmentId(unit.getEquipment().getId());
        response.setEquipmentName(unit.getEquipment().getName());
        response.setLocation(unit.getLocation());
        response.setPurchaseDate(unit.getPurchaseDate());
        response.setWarrantyExpiry(unit.getWarrantyExpiry());
        response.setNotes(unit.getNotes());
        response.setCreatedAt(unit.getCreatedAt());
        
        // Check for active assignment
        assignmentRepository.findActiveByEquipmentUnit(unit)
                .ifPresent(assignment -> {
                    response.setAssigned(true);
                    response.setAssignedTo(assignment.getAssignedToName());
                    response.setAssignmentType(assignment.getAssignmentType().name());
                });
        
        return response;
    }

    private EquipmentAssignmentResponse mapAssignmentToResponse(EquipmentAssignment assignment) {
        EquipmentAssignmentResponse response = new EquipmentAssignmentResponse();
        response.setId(assignment.getId());
        response.setEquipmentUnitId(assignment.getEquipmentUnit().getId());
        response.setSerialNumber(assignment.getEquipmentUnit().getSerialNumber());
        response.setEquipmentName(assignment.getEquipmentUnit().getEquipmentName());
        response.setAssignmentType(assignment.getAssignmentType());
        response.setAssignedToName(assignment.getAssignedToName());
        response.setAssignmentPeriod(assignment.getAssignmentPeriod());
        response.setStartDate(assignment.getStartDate());
        response.setEndDate(assignment.getEndDate());
        response.setStatus(assignment.getStatus());
        response.setAssignedBy(assignment.getAssignedBy().getFullName());
        response.setAssignedAt(assignment.getAssignedAt());
        response.setReturnReason(assignment.getReturnReason());
        response.setReturnedAt(assignment.getReturnedAt());
        return response;
    }
}