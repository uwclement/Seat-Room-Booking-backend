package com.auca.library.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.auca.library.dto.request.EquipmentRequest;
import com.auca.library.dto.request.EquipmentRequestApprovalRequest;
import com.auca.library.dto.request.EquipmentStatusUpdateRequest;
import com.auca.library.dto.response.EquipmentInventoryResponse;
import com.auca.library.dto.response.EquipmentRequestResponse;
import com.auca.library.dto.response.EquipmentResponse;
import com.auca.library.dto.response.MessageResponse;
import com.auca.library.exception.ResourceNotFoundException;
import com.auca.library.model.Equipment;
import com.auca.library.model.EquipmentAssignment;
import com.auca.library.model.EquipmentInventory;
import com.auca.library.model.EquipmentLog;
import com.auca.library.model.EquipmentStatus;
import com.auca.library.model.EquipmentUnit;
import com.auca.library.model.Location;
import com.auca.library.model.User;
import com.auca.library.repository.EquipmentAssignmentRepository;
import com.auca.library.repository.EquipmentInventoryRepository;
import com.auca.library.repository.EquipmentLogRepository;
import com.auca.library.repository.EquipmentRepository;
import com.auca.library.repository.EquipmentRequestRepository;
import com.auca.library.repository.EquipmentUnitRepository;
import com.auca.library.repository.UserRepository;

@Service
public class EquipmentService {

    @Autowired
    private EquipmentRepository equipmentRepository;
    
    @Autowired
    private EquipmentInventoryRepository inventoryRepository;
    
    @Autowired
    private EquipmentLogRepository logRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EquipmentUnitRepository equipmentUnitRepository;
    
    @Autowired
    private EquipmentAssignmentRepository assignmentRepository;

    @Autowired
    private EquipmentRequestRepository equipmentRequestRepository;

    @Autowired
    private NotificationService notificationService;

    

    // Location-based methods for equipment admin
    public List<EquipmentResponse> getAllEquipmentByLocation(Location location) {
        return equipmentRepository.findByLocation(location).stream()
                .map(this::mapToResponseWithInventory)
                .collect(Collectors.toList());
    }

    public List<EquipmentResponse> getAvailableEquipmentByLocation(Location location) {
        return equipmentRepository.findByLocationAndAvailableTrue(location).stream()
                .map(this::mapToResponseWithInventory)
                .collect(Collectors.toList());
    }

    public List<EquipmentResponse> getStudentAllowedEquipmentByLocation(Location location) {
        return equipmentRepository.findStudentAllowedAndAvailableByLocation(location).stream()
                .map(this::mapToResponseWithInventory)
                .collect(Collectors.toList());
    }

    // Original methods for backward compatibility
    public List<EquipmentResponse> getAllEquipment() {
        return equipmentRepository.findAll().stream()
                .map(this::mapToResponseWithInventory)
                .collect(Collectors.toList());
    }

    public List<EquipmentResponse> getAvailableEquipment() {
        return equipmentRepository.findByAvailableTrue().stream()
                .map(this::mapToResponseWithInventory)
                .collect(Collectors.toList());
    }

    public List<EquipmentResponse> getStudentAllowedEquipment() {
        return equipmentRepository.findAll().stream()
                .filter(eq -> eq.isAllowedToStudents() && eq.isAvailable())
                .map(this::mapToResponseWithInventory)
                .collect(Collectors.toList());
    }

    public EquipmentResponse getEquipmentById(Long id) {
        Equipment equipment = findEquipmentById(id);
        return mapToResponseWithInventory(equipment);
    }

    // Validate user can access equipment (same location)
    public void validateEquipmentAccess(Equipment equipment, User user) {
        if (!equipment.getLocation().equals(user.getLocation())) {
            throw new IllegalArgumentException("You can only access equipment from your location");
        }
    }

    @Transactional
    public EquipmentResponse createEquipment(EquipmentRequest request, User user) {
        // Validate location matches user's location
        if (!request.getLocation().equals(user.getLocation())) {
            throw new IllegalArgumentException("You can only create equipment for your location");
        }

        if (equipmentRepository.existsByName(request.getName())) {
            throw new IllegalArgumentException("Equipment with name '" + request.getName() + "' already exists");
        }

        Equipment equipment = new Equipment(request.getName(), request.getDescription(), request.getLocation());
        equipment.setAvailable(request.isAvailable());
        equipment.setAllowedToStudents(request.isAllowedToStudents());
        
        // Set quantity fields for backward compatibility
        if (request.getQuantity() != null && request.getQuantity() > 0) {
            equipment.setQuantity(request.getQuantity());
            equipment.setAvailableQuantity(request.getQuantity());
        }
        
        equipment = equipmentRepository.save(equipment);

        // Initialize inventory - all quantity goes to AVAILABLE status
        Integer initialQuantity = request.getQuantity() != null ? request.getQuantity() : 1;
        initializeEquipmentInventory(equipment, initialQuantity, user);
        
        return mapToResponseWithInventory(equipment);
    }

    @Transactional
    public EquipmentResponse updateEquipment(Long id, EquipmentRequest request, User user) {
        Equipment equipment = findEquipmentById(id);
        validateEquipmentAccess(equipment, user);

        // Check if name is being changed and conflicts
        if (!equipment.getName().equals(request.getName()) && 
            equipmentRepository.existsByName(request.getName())) {
            throw new IllegalArgumentException("Equipment with name '" + request.getName() + "' already exists");
        }

        equipment.setName(request.getName());
        equipment.setDescription(request.getDescription());
        equipment.setAvailable(request.isAvailable());
        equipment.setAllowedToStudents(request.isAllowedToStudents());
        
        // Update quantity - be careful with inventory
        if (request.getQuantity() != null && request.getQuantity() > 0) {
            updateEquipmentTotalQuantity(equipment, request.getQuantity(), user);
        }

        equipment = equipmentRepository.save(equipment);
        return mapToResponseWithInventory(equipment);
    }

    @Transactional
    public MessageResponse deleteEquipment(Long id, User user) {
        Equipment equipment = findEquipmentById(id);
        validateEquipmentAccess(equipment, user);
        
        // Delete related inventory and logs
        List<EquipmentInventory> inventories = inventoryRepository.findByEquipment(equipment);
        inventoryRepository.deleteAll(inventories);
        
        // DELETE RELATED EQUIPMENT UNITS AND ASSIGNMENTS
        List<EquipmentUnit> units = equipmentUnitRepository.findByEquipment(equipment);
        for (EquipmentUnit unit : units) {
            List<EquipmentAssignment> assignments = assignmentRepository.findByEquipmentUnit(unit);
            assignmentRepository.deleteAll(assignments);
        }
        equipmentUnitRepository.deleteAll(units);
        
        equipmentRepository.delete(equipment);
        return new MessageResponse("Equipment deleted successfully");
    }

    // NEW: Move quantities between statuses
    @Transactional
    public EquipmentResponse updateEquipmentStatus(Long id, EquipmentStatusUpdateRequest request, User user) {
        Equipment equipment = findEquipmentById(id);
        validateEquipmentAccess(equipment, user);

        // Find source inventory
        EquipmentInventory fromInventory = inventoryRepository
                .findByEquipmentAndStatus(equipment, request.getFromStatus())
                .orElseThrow(() -> new IllegalArgumentException("No inventory found for status: " + request.getFromStatus()));

        // Check if enough quantity available
        if (fromInventory.getQuantity() < request.getQuantity()) {
            throw new IllegalArgumentException("Not enough quantity available. Current: " + fromInventory.getQuantity() + ", Requested: " + request.getQuantity());
        }

        // Update source inventory
        fromInventory.setQuantity(fromInventory.getQuantity() - request.getQuantity());
        inventoryRepository.save(fromInventory);

        // Find or create target inventory
        EquipmentInventory toInventory = inventoryRepository
                .findByEquipmentAndStatus(equipment, request.getToStatus())
                .orElse(new EquipmentInventory(equipment, request.getToStatus(), 0));
        
        toInventory.setQuantity(toInventory.getQuantity() + request.getQuantity());
        inventoryRepository.save(toInventory);

        // Update equipment available quantity for backward compatibility
        updateEquipmentAvailableQuantity(equipment);

        // Log the change
        EquipmentLog log = new EquipmentLog(equipment, request.getFromStatus(), request.getToStatus(), 
                                          request.getQuantity(), user, request.getNotes());
        logRepository.save(log);

        return mapToResponseWithInventory(equipment);
    }

    // Get equipment history
    public List<EquipmentLog> getEquipmentHistory(Long id, User user) {
        Equipment equipment = findEquipmentById(id);
        validateEquipmentAccess(equipment, user);
        return logRepository.findByEquipmentOrderByChangedAtDesc(equipment);
    }

    // Search equipment by location
    public List<EquipmentResponse> searchEquipmentByLocation(String keyword, Location location) {
        return equipmentRepository.searchEquipmentByLocation(keyword, location).stream()
                .map(this::mapToResponseWithInventory)
                .collect(Collectors.toList());
    }

    // Helper Methods
    private void initializeEquipmentInventory(Equipment equipment, Integer initialQuantity, User user) {
        // Create inventory records for all statuses
        for (EquipmentStatus status : EquipmentStatus.values()) {
            Integer quantity = status == EquipmentStatus.AVAILABLE ? initialQuantity : 0;
            EquipmentInventory inventory = new EquipmentInventory(equipment, status, quantity);
            inventoryRepository.save(inventory);
        }

        // Log initial creation
        EquipmentLog log = new EquipmentLog(equipment, null, EquipmentStatus.AVAILABLE, 
                                          initialQuantity, user, "Initial equipment creation");
        logRepository.save(log);
    }

    private void updateEquipmentTotalQuantity(Equipment equipment, Integer newTotalQuantity, User user) {
        Integer currentTotal = inventoryRepository.getTotalQuantityForEquipment(equipment);
        
        if (newTotalQuantity > currentTotal) {
            // Add quantity to AVAILABLE status
            Integer additionalQuantity = newTotalQuantity - currentTotal;
            EquipmentInventory availableInventory = inventoryRepository
                    .findByEquipmentAndStatus(equipment, EquipmentStatus.AVAILABLE)
                    .orElse(new EquipmentInventory(equipment, EquipmentStatus.AVAILABLE, 0));
            
            availableInventory.setQuantity(availableInventory.getQuantity() + additionalQuantity);
            inventoryRepository.save(availableInventory);

            // Log the addition
            EquipmentLog log = new EquipmentLog(equipment, null, EquipmentStatus.AVAILABLE, 
                                              additionalQuantity, user, "Quantity increased");
            logRepository.save(log);
            
        } else if (newTotalQuantity < currentTotal) {
            Integer reductionQuantity = currentTotal - newTotalQuantity;
            EquipmentInventory availableInventory = inventoryRepository
                .findByEquipmentAndStatus(equipment, EquipmentStatus.AVAILABLE)
                .orElseThrow(() -> new IllegalArgumentException("No available inventory found"));

            if (availableInventory.getQuantity() < reductionQuantity) {
                throw new IllegalArgumentException("Not enough available quantity to reduce");
            }

            availableInventory.setQuantity(availableInventory.getQuantity() - reductionQuantity);
            inventoryRepository.save(availableInventory);

            // Log the reduction
            EquipmentLog log = new EquipmentLog(equipment, EquipmentStatus.AVAILABLE, EquipmentStatus.AVAILABLE,
                                                reductionQuantity, user, "Quantity reduced");
            logRepository.save(log);
        }

        equipment.setQuantity(newTotalQuantity);
        updateEquipmentAvailableQuantity(equipment);
    }

    private void updateEquipmentAvailableQuantity(Equipment equipment) {
        Integer availableQty = inventoryRepository.getAvailableQuantityForEquipment(equipment);
        equipment.setAvailableQuantity(availableQty != null ? availableQty : 0);
        equipmentRepository.save(equipment);
    }

     private Equipment findEquipmentById(Long id) {
        return equipmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Equipment not found: " + id));
    }

    private EquipmentResponse mapToResponseWithInventory(Equipment equipment) {
        EquipmentResponse response = new EquipmentResponse();
        response.setId(equipment.getId());
        response.setName(equipment.getName());
        response.setDescription(equipment.getDescription());
        response.setAvailable(equipment.isAvailable());
        response.setAllowedToStudents(equipment.isAllowedToStudents());
        response.setQuantity(equipment.getQuantity());
        response.setAvailableQuantity(equipment.getAvailableQuantity());
        response.setLocation(equipment.getLocation());

        // Get inventory breakdown
        List<EquipmentInventory> inventories = inventoryRepository.findByEquipment(equipment);
        List<EquipmentInventoryResponse> inventoryResponses = inventories.stream()
                .map(this::mapInventoryToResponse)
                .collect(Collectors.toList());
        
        response.setInventoryBreakdown(inventoryResponses);
        response.setTotalQuantity(inventoryRepository.getTotalQuantityForEquipment(equipment));
        response.setActualAvailableQuantity(inventoryRepository.getAvailableQuantityForEquipment(equipment));

        return response;
    }

    private EquipmentInventoryResponse mapInventoryToResponse(EquipmentInventory inventory) {
        EquipmentInventoryResponse response = new EquipmentInventoryResponse();
        response.setId(inventory.getId());
        response.setStatus(inventory.getStatus());
        response.setQuantity(inventory.getQuantity());
        return response;
    }

    // Legacy methods for backward compatibility
    @Transactional
    public EquipmentResponse toggleEquipmentAvailability(Long id) {
        Equipment equipment = findEquipmentById(id);
        equipment.setAvailable(!equipment.isAvailable());
        equipment = equipmentRepository.save(equipment);
        return mapToResponseWithInventory(equipment);
    }

    @Transactional
    public boolean reserveEquipment(Long equipmentId, int quantity) {
        Equipment equipment = findEquipmentById(equipmentId);
        if (equipment.isAvailableInQuantity(quantity)) {
            equipment.setAvailableQuantity(equipment.getAvailableQuantity() - quantity);
            equipmentRepository.save(equipment);
            return true;
        }
        return false;
    }

    @Transactional
    public void releaseEquipment(Long equipmentId, int quantity) {
        Equipment equipment = findEquipmentById(equipmentId);
        int newAvailable = Math.min(equipment.getAvailableQuantity() + quantity, equipment.getQuantity());
        equipment.setAvailableQuantity(newAvailable);
        equipmentRepository.save(equipment);
    }

    public List<EquipmentResponse> searchEquipment(String keyword) {
        return equipmentRepository.searchEquipment(keyword).stream()
                .map(this::mapToResponseWithInventory)
                .collect(Collectors.toList());
    }



    // Enhanced approval method with serial number selection
    @Transactional
    public MessageResponse handleEquipmentRequestApprovalWithSerial(Long requestId, 
            EquipmentRequestApprovalRequest request, Long selectedUnitId, String adminEmail) {
        
        com.auca.library.model.EquipmentRequest equipmentRequest = findRequestById(requestId);
        User admin = findUserByEmail(adminEmail);
        
        if (equipmentRequest.getStatus() != com.auca.library.model.EquipmentRequest.RequestStatus.PENDING) {
            throw new IllegalStateException("Only pending requests can be processed");
        }
        
        equipmentRequest.setApprovedBy(admin);
        equipmentRequest.setApprovedAt(LocalDateTime.now());
        
        if (request.isApproved()) {
            // Validate and assign specific equipment unit
            if (selectedUnitId != null) {
                EquipmentUnit selectedUnit = equipmentUnitRepository.findById(selectedUnitId)
                        .orElseThrow(() -> new ResourceNotFoundException("Equipment unit not found: " + selectedUnitId));
                
                // Validate unit belongs to requested equipment
                if (!selectedUnit.getEquipment().getId().equals(equipmentRequest.getEquipment().getId())) {
                    throw new IllegalArgumentException("Selected unit does not match requested equipment");
                }
                
                // Validate unit is available
                if (!selectedUnit.isAvailable()) {
                    throw new IllegalStateException("Selected equipment unit is not available");
                }
                
                // Create assignment
                EquipmentAssignment assignment = new EquipmentAssignment(selectedUnit, 
                        EquipmentAssignment.AssignmentType.REQUEST_ASSIGNMENT, admin);
                assignment.setEquipmentRequest(equipmentRequest);
                assignmentRepository.save(assignment);
                
                // Update unit status
                selectedUnit.setStatus(EquipmentUnit.UnitStatus.IN_REQUEST);
                equipmentUnitRepository.save(selectedUnit);
                
                // Store selected unit reference in request
                equipmentRequest.setAssignedEquipmentUnit(selectedUnit);
            }
            
            equipmentRequest.setStatus(com.auca.library.model.EquipmentRequest.RequestStatus.APPROVED);
            
            notificationService.addNotification(
                equipmentRequest.getUser().getEmail(),
                "Equipment Request Approved",
                String.format("Your request for %s has been approved. Serial Number: %s", 
                    equipmentRequest.getEquipment().getName(),
                    selectedUnitId != null ? getSerialNumber(selectedUnitId) : "Will be assigned"),
                "EQUIPMENT_APPROVED"
            );
            
            equipmentRequestRepository.save(equipmentRequest);
            return new MessageResponse("Equipment request approved successfully with serial number tracking");
            
        } else {
            equipmentRequest.setStatus(com.auca.library.model.EquipmentRequest.RequestStatus.REJECTED);
            equipmentRequest.setRejectionReason(request.getRejectionReason());
            equipmentRequest.setAdminSuggestion(request.getAdminSuggestion());
            
            String notificationMessage = String.format("Your request for %s has been rejected", 
                equipmentRequest.getEquipment().getName());
            if (request.getRejectionReason() != null) {
                notificationMessage += ". Reason: " + request.getRejectionReason();
            }
            
            notificationService.addNotification(
                equipmentRequest.getUser().getEmail(),
                "Equipment Request Rejected",
                notificationMessage,
                "EQUIPMENT_REJECTED"
            );
        }
        
        equipmentRequestRepository.save(equipmentRequest);
        return new MessageResponse("Equipment request processed successfully");
    }

    // CORRECTED: Method to handle equipment return with serial number verification
    @Transactional
    public MessageResponse returnEquipmentWithSerial(Long requestId, String returnCondition, 
            String returnNotes, String adminEmail) {
        
        com.auca.library.model.EquipmentRequest equipmentRequest = findRequestById(requestId);
        User admin = findUserByEmail(adminEmail);
        
        if (equipmentRequest.getAssignedEquipmentUnit() == null) {
            throw new IllegalStateException("No equipment unit assigned to this request");
        }
        
        if (equipmentRequest.getStatus() != com.auca.library.model.EquipmentRequest.RequestStatus.APPROVED &&
            equipmentRequest.getStatus() != com.auca.library.model.EquipmentRequest.RequestStatus.IN_USE) {
            throw new IllegalStateException("Equipment can only be returned from approved or in-use requests");
        }
        
        EquipmentUnit assignedUnit = equipmentRequest.getAssignedEquipmentUnit();
        
        // Find and close the assignment
        Optional<EquipmentAssignment> activeAssignment = assignmentRepository
                .findActiveByEquipmentUnit(assignedUnit);
        
        if (activeAssignment.isPresent()) {
            EquipmentAssignment assignment = activeAssignment.get();
            assignment.setStatus(EquipmentAssignment.AssignmentStatus.RETURNED);
            assignment.setReturnedAt(LocalDateTime.now());
            assignment.setReturnedBy(admin);
            assignment.setReturnReason("Equipment request completed - " + returnCondition);
            assignmentRepository.save(assignment);
        }
        
        // Update equipment unit status
        if ("DAMAGED".equalsIgnoreCase(returnCondition)) {
            assignedUnit.setStatus(EquipmentUnit.UnitStatus.DAMAGED);
        } else if ("MAINTENANCE".equalsIgnoreCase(returnCondition)) {
            assignedUnit.setStatus(EquipmentUnit.UnitStatus.MAINTENANCE);
        } else {
            assignedUnit.setStatus(EquipmentUnit.UnitStatus.AVAILABLE);
        }
        
        if (returnNotes != null) {
            assignedUnit.setNotes(returnNotes);
        }
        equipmentUnitRepository.save(assignedUnit);
        
        // Update request
        equipmentRequest.setStatus(com.auca.library.model.EquipmentRequest.RequestStatus.RETURNED);
        equipmentRequest.setReturnedAt(LocalDateTime.now());
        equipmentRequest.setReturnedBy(admin);
        equipmentRequest.setReturnCondition(returnCondition);
        equipmentRequest.setReturnNotes(returnNotes);
        equipmentRequestRepository.save(equipmentRequest);
        
        // Notify requester
        notificationService.addNotification(
            equipmentRequest.getUser().getEmail(),
            "Equipment Returned",
            String.format("Equipment %s (Serial: %s) has been returned. Condition: %s", 
                equipmentRequest.getEquipment().getName(),
                assignedUnit.getSerialNumber(),
                returnCondition),
            "EQUIPMENT_RETURNED"
        );
        
        return new MessageResponse("Equipment returned successfully with serial number: " + assignedUnit.getSerialNumber());
    }

    // Get available equipment units for approval
    public List<EquipmentUnit> getAvailableUnitsForRequest(Long requestId, String adminEmail) {
        com.auca.library.model.EquipmentRequest request = findRequestById(requestId);
        User admin = findUserByEmail(adminEmail);
        
        // Validate admin can access this equipment's location
        if (!admin.getRoles().stream().anyMatch(role -> role.getName().name().equals("ROLE_ADMIN"))) {
            if (!admin.getLocation().equals(request.getEquipment().getLocation())) {
                throw new SecurityException("Cannot access equipment from different location");
            }
        }
        
        return equipmentUnitRepository.findAvailableByEquipment(request.getEquipment());
    }

    // Enhanced response mapping to include serial number info
    private EquipmentRequestResponse mapToResponseWithSerial(com.auca.library.model.EquipmentRequest request) {
        EquipmentRequestResponse response = mapToResponse(request); // Your existing method
        
        // Add serial number information if equipment is assigned
        if (request.getAssignedEquipmentUnit() != null) {
            response.setAssignedSerialNumber(request.getAssignedEquipmentUnit().getSerialNumber());
            response.setAssignedUnitId(request.getAssignedEquipmentUnit().getId());
            response.setAssignedUnitCondition(request.getAssignedEquipmentUnit().getCondition());
            response.setHasSerialNumberAssigned(true);
        }
        
        return response;
    }

    // Helper method to get serial number
    private String getSerialNumber(Long unitId) {
        return equipmentUnitRepository.findById(unitId)
                .map(EquipmentUnit::getSerialNumber)
                .orElse("Unknown");
    }

    // Check equipment availability for new requests
    public boolean isEquipmentAvailableForRequest(Long equipmentId, int requestedQuantity, 
            LocalDateTime startTime, LocalDateTime endTime) {
        
        Equipment equipment = findEquipmentById(equipmentId);
        
        // Count available units
        List<EquipmentUnit> availableUnits = equipmentUnitRepository.findAvailableByEquipment(equipment);
        
        // Count units that will be available during the requested time
        List<com.auca.library.model.EquipmentRequest> conflictingRequests = equipmentRequestRepository
                .findConflictingRequests(equipmentId, startTime, endTime);
        
        int reservedDuringPeriod = conflictingRequests.stream()
                .mapToInt(com.auca.library.model.EquipmentRequest::getRequestedQuantity)
                .sum();
        
        int actuallyAvailable = availableUnits.size() - reservedDuringPeriod;
        
        return actuallyAvailable >= requestedQuantity;
    }

    // Bulk assignment for multiple requests
    @Transactional
    public MessageResponse bulkApproveRequestsWithSerials(List<Long> requestIds, 
            Map<Long, Long> requestToUnitMapping, String adminEmail) {
        
        User admin = findUserByEmail(adminEmail);
        int successCount = 0;
        List<String> errors = new ArrayList<>();
        
        for (Long requestId : requestIds) {
            try {
                Long unitId = requestToUnitMapping.get(requestId);
                if (unitId != null) {
                    EquipmentRequestApprovalRequest approvalRequest = new EquipmentRequestApprovalRequest();
                    approvalRequest.setApproved(true);
                    
                    handleEquipmentRequestApprovalWithSerial(requestId, approvalRequest, unitId, adminEmail);
                    successCount++;
                }
            } catch (Exception e) {
                errors.add("Request " + requestId + ": " + e.getMessage());
            }
        }
        
        if (errors.isEmpty()) {
            return new MessageResponse(successCount + " requests approved successfully");
        } else {
            return new MessageResponse(successCount + " requests approved, " + errors.size() + 
                    " failed: " + String.join("; ", errors));
        }
    }

    // HELPER METHODS (your existing ones)
    private User findUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + email));
    }


    private com.auca.library.model.EquipmentRequest findRequestById(Long id) {
        return equipmentRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Equipment request not found: " + id));
    }

    // Your existing methods for getting admin emails
    private String getEquipmentAdminEmail() {
        try {
            return userRepository.findEquipmentAdmin()
                    .map(User::getEmail)
                    .orElse(null);
        } catch (IncorrectResultSizeDataAccessException e) {
            // Fallback: get first equipment admin
            List<User> admins = userRepository.findAllEquipmentAdmins();
            return admins.isEmpty() ? null : admins.get(0).getEmail();
        }
    }

    private String getHodEmail() {
        try {
            return userRepository.findHod()
                    .map(User::getEmail)
                    .orElse(null);
        } catch (IncorrectResultSizeDataAccessException e) {
            // Fallback: get first HOD
            List<User> hods = userRepository.findAllHods();
            return hods.isEmpty() ? null : hods.get(0).getEmail();
        }
    }

    // Your existing mapToResponse method - keep it as is, just add this enhanced version
    private EquipmentRequestResponse mapToResponse(com.auca.library.model.EquipmentRequest request) {
        EquipmentRequestResponse response = new EquipmentRequestResponse();

        response.setId(request.getId());
        response.setEquipmentId(request.getEquipment().getId());
        response.setEquipmentName(request.getEquipment().getName());
        response.setReason(request.getReason());
        response.setStartTime(request.getStartTime());
        response.setEndTime(request.getEndTime());
        response.setRequestedQuantity(request.getRequestedQuantity());
        response.setStatus(request.getStatus());
        response.setRejectionReason(request.getRejectionReason());
        response.setAdminSuggestion(request.getAdminSuggestion());
        response.setCreatedAt(request.getCreatedAt());
        response.setApprovedAt(request.getApprovedAt());
        response.setEscalatedToHod(request.isEscalatedToHod());
        response.setEscalatedAt(request.getEscalatedAt());
        response.setUserId(request.getUser().getId());
        response.setUserFullName(request.getUser().getFullName());

        // Add duration calculation
        if (request.getStartTime() != null && request.getEndTime() != null) {
            Duration duration = Duration.between(request.getStartTime(), request.getEndTime());
            response.setDurationHours(duration.toHours());
        } else {
            response.setDurationHours(0L);
        }
        
        if (request.getCourse() != null) {
            response.setCourseId(request.getCourse().getId());
            response.setCourseCode(request.getCourse().getCourseCode());
            response.setCourseName(request.getCourse().getCourseName());
        }
        
        if (request.getLabClass() != null) {
            response.setLabClassId(request.getLabClass().getId());
            response.setLabClassName(request.getLabClass().getName());
        }
        
        if (request.getApprovedBy() != null) {
            response.setApprovedByName(request.getApprovedBy().getFullName());
        }

        // Add serial number information if equipment is assigned
        if (request.getAssignedEquipmentUnit() != null) {
            response.setAssignedSerialNumber(request.getAssignedEquipmentUnit().getSerialNumber());
            response.setAssignedUnitId(request.getAssignedEquipmentUnit().getId());
            response.setAssignedUnitCondition(request.getAssignedEquipmentUnit().getCondition());
            response.setHasSerialNumberAssigned(true);
        }

        // All your existing response fields...
        response.setSuggestionAcknowledged(request.getSuggestionAcknowledged());
        response.setSuggestionResponseReason(request.getSuggestionResponseReason());
        response.setSuggestionResponseAt(request.getSuggestionResponseAt());
        response.setCanRespondToSuggestion(
            request.getAdminSuggestion() != null && 
            request.getSuggestionAcknowledged() == null && 
            request.getStatus() != com.auca.library.model.EquipmentRequest.RequestStatus.HOD_REJECTED
        );
        
        // Return fields
        response.setReturnedAt(request.getReturnedAt());
        if (request.getReturnedBy() != null) {
            response.setReturnedByName(request.getReturnedBy().getFullName());
        }
        response.setReturnCondition(request.getReturnCondition());
        response.setReturnNotes(request.getReturnNotes());
        response.setEarlyReturn(request.getIsEarlyReturn() != null ? request.getIsEarlyReturn() : false);
        response.setLateReturn(request.getIsLateReturn() != null ? request.getIsLateReturn() : false);
        response.setCanMarkReturned(
            request.getReturnedAt() == null && 
            (request.getStatus() == com.auca.library.model.EquipmentRequest.RequestStatus.APPROVED ||
             request.getStatus() == com.auca.library.model.EquipmentRequest.RequestStatus.IN_USE ||
             request.getStatus() == com.auca.library.model.EquipmentRequest.RequestStatus.HOD_APPROVED)
        );
        
        // Extension fields (keep your existing extension logic)
        response.setTotalExtensionsToday(request.getTotalExtensionsToday() != null ? request.getTotalExtensionsToday().doubleValue() : 0.0);
        response.setTotalExtensionHoursToday(request.getTotalExtensionHoursToday() != null ? request.getTotalExtensionHoursToday() : 0.0);
        response.setRemainingExtensionHours(3.0 - (request.getTotalExtensionHoursToday() != null ? request.getTotalExtensionHoursToday() : 0.0));
        response.setExtensionReason(request.getExtensionReason());
        response.setExtensionStatus(request.getExtensionStatus());
        response.setExtensionRequestedAt(request.getExtensionRequestedAt());
        response.setExtensionApprovedAt(request.getExtensionApprovedAt());
        response.setExtensionHoursRequested(request.getExtensionHoursRequested());
        if (request.getExtensionApprovedBy() != null) {
            response.setExtensionApprovedByName(request.getExtensionApprovedBy().getFullName());
        }
        response.setOriginalEndTime(request.getOriginalEndTime());
        response.setHasActiveExtension(request.getExtensionStatus() != null && !request.getExtensionStatus().equals("REJECTED"));
        
        // Action availability flags
        LocalDateTime now = LocalDateTime.now();
        response.setCanRequestExtension(
            request.getExtensionStatus() == null || request.getExtensionStatus().equals("REJECTED") &&
            (request.getStatus() == com.auca.library.model.EquipmentRequest.RequestStatus.APPROVED ||
             request.getStatus() == com.auca.library.model.EquipmentRequest.RequestStatus.IN_USE) &&
            response.getRemainingExtensionHours() > 0
        );
        
        response.setCanApproveExtension(request.getExtensionStatus() != null && request.getExtensionStatus().equals("PENDING"));
        
        response.setCanCancel(request.getStatus() == com.auca.library.model.EquipmentRequest.RequestStatus.PENDING);
        
        response.setCanEscalateToHod(
            request.getStatus() == com.auca.library.model.EquipmentRequest.RequestStatus.REJECTED && 
            !request.isEscalatedToHod()
        );
        
        // Check for conflicts if extension is pending
        if (request.getExtensionStatus() != null && request.getExtensionStatus().equals("PENDING")) {
            LocalDateTime newEndTime = request.getEndTime()
                .plusHours(request.getExtensionHoursRequested().longValue())
                .plusMinutes((long)((request.getExtensionHoursRequested() % 1) * 60));
            
            List<com.auca.library.model.EquipmentRequest> conflicts = equipmentRequestRepository.findConflictingRequests(
                request.getEquipment().getId(), request.getEndTime(), newEndTime);
            
            response.setHasConflicts(!conflicts.isEmpty());
            response.setConflictingRequestsCount(conflicts.size());
            if (!conflicts.isEmpty()) {
                response.setConflictDetails(String.format("%d conflicting booking(s) in extended time slot", conflicts.size()));
            }
        }

        if (hasRole(request.getUser(), "ROLE_PROFESSOR")) {
            double usedToday = getTotalExtensionHoursToday(request.getUser().getId());
            response.setTotalExtensionHoursToday(usedToday);
            response.setRemainingExtensionHours(3.0 - usedToday);
        }
        
        // UI section visibility flags
        response.setShowReturnSection(response.isCanMarkReturned());
        response.setShowExtensionSection(response.isCanRequestExtension() || response.isCanApproveExtension());
            
        return response;
    }

    // Helper methods from your existing code
    private boolean hasRole(User user, String roleName) {
        return user.getRoles().stream()
                .anyMatch(role -> role.getName().name().equals(roleName));
    }

    private double getTotalExtensionHoursToday(Long userId) {
        LocalDateTime startOfDay = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime endOfDay = startOfDay.plusDays(1).minusNanos(1);
        
        return equipmentRequestRepository.getTotalExtensionHoursForUserToday(userId, startOfDay, endOfDay);
    }
}