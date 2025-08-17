package com.auca.library.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.auca.library.dto.request.EquipmentRequestApprovalRequest;
import com.auca.library.dto.request.EquipmentRequestRequest;
import com.auca.library.dto.response.EquipmentRequestResponse;
import com.auca.library.dto.response.MessageResponse;
import com.auca.library.exception.ResourceNotFoundException;
import com.auca.library.model.Course;
import com.auca.library.model.Equipment;
import com.auca.library.model.EquipmentRequest;
import com.auca.library.model.LabClass;
import com.auca.library.model.User;
import com.auca.library.repository.CourseRepository;
import com.auca.library.repository.EquipmentRepository;
import com.auca.library.repository.EquipmentRequestRepository;
import com.auca.library.repository.LabClassRepository;
import com.auca.library.repository.RoomBookingRepository;
import com.auca.library.repository.UserRepository;

@Service
public class EquipmentRequestService {

    @Autowired
    private EquipmentRequestRepository equipmentRequestRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private EquipmentRepository equipmentRepository;
    
    @Autowired
    private CourseRepository courseRepository;
    
    @Autowired
    private LabClassRepository labClassRepository;
    
    @Autowired
    private RoomBookingRepository roomBookingRepository;
    
    @Autowired
    private EquipmentService equipmentService;
    
    @Autowired
    private NotificationService notificationService;

    // Create standalone equipment request (professors)
    @Transactional
    public EquipmentRequestResponse createEquipmentRequest(EquipmentRequestRequest request, String userEmail) {
        User user = findUserByEmail(userEmail);
        Equipment equipment = findEquipmentById(request.getEquipmentId());
        
        // Validate user can make this request
        validateUserCanRequestEquipment(user, equipment);
        
        // For professors, course is required
        Course course = null;
        if (hasRole(user, "ROLE_PROFESSOR")) {
            if (request.getCourseId() == null) {
                throw new IllegalArgumentException("Course is required for professor equipment requests");
            }
            course = findCourseById(request.getCourseId());
            validateProfessorCourseAssociation(user, course);
        }
        
        // Check equipment availability
        if (!equipment.isAvailableInQuantity(request.getRequestedQuantity())) {
            throw new IllegalArgumentException("Requested quantity not available");
        }

        if (request.getRequestedQuantity() <= 0) {
            throw new IllegalArgumentException("Requested quantity must be positive");
        }
        if (request.getReason() == null || request.getReason().trim().isEmpty()) {
            throw new IllegalArgumentException("Reason is required");
        }
        
        // Check for time conflicts
        List<EquipmentRequest> conflicts = equipmentRequestRepository.findConflictingRequests(
            equipment.getId(), request.getStartTime(), request.getEndTime());
        
        int totalRequestedInTimeSlot = conflicts.stream()
            .mapToInt(EquipmentRequest::getRequestedQuantity)
            .sum() + request.getRequestedQuantity();
            
        if (totalRequestedInTimeSlot > equipment.getQuantity()) {
            throw new IllegalArgumentException("Equipment not available for requested time slot");
        }
        
        // Create request
        EquipmentRequest equipmentRequest = new EquipmentRequest();
        equipmentRequest.setUser(user);
        equipmentRequest.setEquipment(equipment);
        equipmentRequest.setCourse(course);
        equipmentRequest.setReason(request.getReason());
        equipmentRequest.setStartTime(request.getStartTime());
        equipmentRequest.setEndTime(request.getEndTime());
        equipmentRequest.setRequestedQuantity(request.getRequestedQuantity());
        
        // Handle lab class if specified
        if (request.getLabClassId() != null) {
            LabClass labClass = findLabClassById(request.getLabClassId());
            equipmentRequest.setLabClass(labClass);
        }
        
        equipmentRequest = equipmentRequestRepository.save(equipmentRequest);
        
        // Reserve equipment quantity immediately
        equipmentService.reserveEquipment(equipment.getId(), request.getRequestedQuantity());
        
        // Send notification to equipment admin
        notificationService.addNotification(
            getEquipmentAdminEmail(),
            "New Equipment Request",
            String.format("New equipment request for %s by %s", equipment.getName(), user.getFullName()),
            "EQUIPMENT_REQUEST"
        );
        
        return mapToResponse(equipmentRequest);
    }

    // Approve/Reject equipment request (Equipment Admin)
    @Transactional
    public MessageResponse handleEquipmentRequestApproval(Long requestId, EquipmentRequestApprovalRequest request, String adminEmail) {
        EquipmentRequest equipmentRequest = findRequestById(requestId);
        User admin = findUserByEmail(adminEmail);
        
       if (equipmentRequest.getStatus() != EquipmentRequest.RequestStatus.PENDING) {
            throw new IllegalStateException("Only pending requests can be processed");
        }
        
        equipmentRequest.setApprovedBy(admin);
        equipmentRequest.setApprovedAt(LocalDateTime.now());
        
        if (request.isApproved()) {
            equipmentRequest.setStatus(EquipmentRequest.RequestStatus.APPROVED);
            
            notificationService.addNotification(
                equipmentRequest.getUser().getEmail(),
                "Equipment Request Approved",
                String.format("Your request for %s has been approved", 
                    equipmentRequest.getEquipment().getName()),
                "EQUIPMENT_APPROVED"
            );
            
            return new MessageResponse("Equipment request approved successfully");
            
        } else {
            equipmentRequest.setStatus(EquipmentRequest.RequestStatus.REJECTED);
            equipmentRequest.setRejectionReason(request.getRejectionReason());
            equipmentRequest.setAdminSuggestion(request.getAdminSuggestion());
            
            // Release reserved equipment
            equipmentService.releaseEquipment(
                equipmentRequest.getEquipment().getId(), 
                equipmentRequest.getRequestedQuantity()
            );
            
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

    // Escalate to HOD (Professor only)
    @Transactional
    public MessageResponse escalateToHod(Long requestId, String professorEmail) {
        EquipmentRequest equipmentRequest = findRequestById(requestId);
        User professor = findUserByEmail(professorEmail);
        
        // Validate escalation eligibility
        if (!equipmentRequest.getUser().equals(professor)) {
            throw new IllegalArgumentException("You can only escalate your own requests");
        }
        
        if (equipmentRequest.getStatus() != EquipmentRequest.RequestStatus.REJECTED) {
            throw new IllegalStateException("Only rejected requests can be escalated");
        }
        
        if (!hasRole(professor, "ROLE_PROFESSOR")) {
            throw new IllegalArgumentException("Only professors can escalate requests");
        }
        
        equipmentRequest.setStatus(EquipmentRequest.RequestStatus.ESCALATED);
        equipmentRequest.setEscalatedToHod(true);
        equipmentRequest.setEscalatedAt(LocalDateTime.now());
        
        equipmentRequestRepository.save(equipmentRequest);
        
        // Notify HOD
        notificationService.addNotification(
            getHodEmail(),
            "Equipment Request Escalation",
            String.format("Professor %s has escalated a rejected equipment request for %s", 
                professor.getFullName(), equipmentRequest.getEquipment().getName()),
            "EQUIPMENT_ESCALATION"
        );
        
        return new MessageResponse("Request escalated to HOD successfully");
    }

    // HOD review escalated request
    @Transactional
    public MessageResponse hodReviewEscalation(Long requestId, EquipmentRequestApprovalRequest request, String hodEmail) {
        EquipmentRequest equipmentRequest = findRequestById(requestId);
        User hod = findUserByEmail(hodEmail);
        
        if (equipmentRequest.getStatus() != EquipmentRequest.RequestStatus.ESCALATED) {
            throw new IllegalStateException("Request is not escalated");
        }
        
        equipmentRequest.setHodReviewedBy(hod);
        equipmentRequest.setHodReviewedAt(LocalDateTime.now());
        
        if (request.isApproved()) {
            equipmentRequest.setStatus(EquipmentRequest.RequestStatus.HOD_APPROVED);
            
            // Re-reserve equipment if available
            if (equipmentRequest.getEquipment().isAvailableInQuantity(equipmentRequest.getRequestedQuantity())) {
                equipmentService.reserveEquipment(
                    equipmentRequest.getEquipment().getId(), 
                    equipmentRequest.getRequestedQuantity()
                );
            }
            
            // Notify professor
            notificationService.addNotification(
                equipmentRequest.getUser().getEmail(),
                "Equipment Request Approved by HOD",
                String.format("Your escalated request for %s has been approved by HOD", 
                    equipmentRequest.getEquipment().getName()),
                "EQUIPMENT_HOD_APPROVED"
            );
            
            // Notify equipment admin to proceed
            notificationService.addNotification(
                getEquipmentAdminEmail(),
                "HOD Approved Equipment Request",
                String.format("HOD has approved the escalated request for %s. Please proceed with assignment.", 
                    equipmentRequest.getEquipment().getName()),
                "EQUIPMENT_HOD_DIRECTIVE"
            );
            
        } else {
            equipmentRequest.setStatus(EquipmentRequest.RequestStatus.HOD_REJECTED);
            equipmentRequest.setRejectionReason(request.getRejectionReason());
            
            notificationService.addNotification(
                equipmentRequest.getUser().getEmail(),
                "Equipment Request Finally Rejected",
                String.format("Your escalated request for %s has been rejected by HOD", 
                    equipmentRequest.getEquipment().getName()),
                "EQUIPMENT_HOD_REJECTED"
            );
        }
        
        equipmentRequestRepository.save(equipmentRequest);
        return new MessageResponse("Escalation reviewed successfully");
    }

    // Get requests for current user
    public List<EquipmentRequestResponse> getCurrentUserRequests(String userEmail) {
        User user = findUserByEmail(userEmail);
            List<EquipmentRequest> requests = equipmentRequestRepository.findByUserOrderByCreatedAtDesc(user);
    
            // Update statuses on-demand
            requests.forEach(this::updateRequestStatusIfNeeded);
    
     return requests.stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
    }

    // Get pending requests (Equipment Admin)
    public List<EquipmentRequestResponse> getPendingRequests() {
        return equipmentRequestRepository.findPendingRequests().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // Get escalated requests (HOD)
    public List<EquipmentRequestResponse> getEscalatedRequests() {
        return equipmentRequestRepository.findEscalatedRequests().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // Complete request when time period ends
    @Transactional
    public void completeRequest(Long requestId) {
        EquipmentRequest request = findRequestById(requestId);
        if (request.getStatus() == EquipmentRequest.RequestStatus.APPROVED || 
            request.getStatus() == EquipmentRequest.RequestStatus.HOD_APPROVED) {
            
            request.setStatus(EquipmentRequest.RequestStatus.COMPLETED);
            equipmentRequestRepository.save(request);
            
            // Release equipment
            equipmentService.releaseEquipment(
                request.getEquipment().getId(), 
                request.getRequestedQuantity()
            );
        }
    }

    // Helper methods
    private void validateUserCanRequestEquipment(User user, Equipment equipment) {
        if (hasRole(user, "ROLE_USER")) { // Student
            if (!equipment.isAllowedToStudents()) {
                throw new IllegalArgumentException("This equipment is not available for student requests");
            }
        }
    }

    private void validateProfessorCourseAssociation(User professor, Course course) {
        if (!professor.getApprovedCourses().contains(course)) {
            throw new IllegalArgumentException("You are not approved to teach this course");
        }
    }

    private boolean hasRole(User user, String roleName) {
        return user.getRoles().stream()
                .anyMatch(role -> role.getName().name().equals(roleName));
    }

     private User findUserByEmail(String email) {
    return userRepository.findByEmail(email)
        .orElseThrow(() -> new ResourceNotFoundException("User not found: " + email));
    }

    private Equipment findEquipmentById(Long id) {
        return equipmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Equipment not found: " + id));
    }

    private Course findCourseById(Long id) {
        return courseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found: " + id));
    }

    private LabClass findLabClassById(Long id) {
        return labClassRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Lab class not found: " + id));
    }

    private EquipmentRequest findRequestById(Long id) {
        return equipmentRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Equipment request not found: " + id));
    }

   
// Temporary fix - modify your service methods to handle multiple results

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


    // Get all equipment requests for the current month (Equipment Admin)
    public List<EquipmentRequestResponse> getCurrentMonthRequests() {
    LocalDateTime startOfMonth = LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
    LocalDateTime endOfMonth = startOfMonth.plusMonths(1).minusNanos(1);
    
    return equipmentRequestRepository.findRequestsInDateRange(startOfMonth, endOfMonth).stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
    }



    
    // Get all equipment requests for the current month (Equipment Admin)
    public List<EquipmentRequestResponse> getHodCurrentMonthRequests() {
    LocalDateTime startOfMonth = LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
    LocalDateTime endOfMonth = startOfMonth.plusMonths(1).minusNanos(1);
    
    return equipmentRequestRepository.findHodRequestsInDateRange(startOfMonth, endOfMonth).stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
    }

    // Cancel request
@Transactional
public MessageResponse cancelRequest(Long requestId, String userEmail) {
    EquipmentRequest request = findRequestById(requestId);
    User user = findUserByEmail(userEmail);
    
    if (!request.getUser().equals(user)) {
        throw new IllegalArgumentException("You can only cancel your own requests");
    }
    
    if (request.getStatus() != EquipmentRequest.RequestStatus.PENDING) {
        throw new IllegalStateException("Only pending requests can be cancelled");
    }
    
    request.setStatus(EquipmentRequest.RequestStatus.CANCELLED);
    equipmentRequestRepository.save(request);
    
    // Release reserved equipment
    equipmentService.releaseEquipment(request.getEquipment().getId(), request.getRequestedQuantity());
    
    return new MessageResponse("Request cancelled successfully");
}

// Get request by ID
public EquipmentRequestResponse getRequestById(Long requestId) {
    EquipmentRequest request = findRequestById(requestId);
    return mapToResponse(request);
}

    private EquipmentRequestResponse mapToResponse(EquipmentRequest request) {
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

        // Suggestion response fields
    response.setSuggestionAcknowledged(request.getSuggestionAcknowledged());
    response.setSuggestionResponseReason(request.getSuggestionResponseReason());
    response.setSuggestionResponseAt(request.getSuggestionResponseAt());
    response.setCanRespondToSuggestion(
        request.getAdminSuggestion() != null && 
        request.getSuggestionAcknowledged() == null && 
        request.getStatus() != EquipmentRequest.RequestStatus.HOD_REJECTED
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
        (request.getStatus() == EquipmentRequest.RequestStatus.APPROVED ||
         request.getStatus() == EquipmentRequest.RequestStatus.IN_USE ||
         request.getStatus() == EquipmentRequest.RequestStatus.HOD_APPROVED)
    );
    
    // Extension fields
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
        (request.getStatus() == EquipmentRequest.RequestStatus.APPROVED ||
         request.getStatus() == EquipmentRequest.RequestStatus.IN_USE) &&
        response.getRemainingExtensionHours() > 0
    );
    
    response.setCanApproveExtension(request.getExtensionStatus() != null && request.getExtensionStatus().equals("PENDING"));
    
    response.setCanCancel(request.getStatus() == EquipmentRequest.RequestStatus.PENDING);
    
    response.setCanEscalateToHod(
        request.getStatus() == EquipmentRequest.RequestStatus.REJECTED && 
        !request.isEscalatedToHod()
    );
    
    // Check for conflicts if extension is pending
    if (request.getExtensionStatus() != null && request.getExtensionStatus().equals("PENDING")) {
        LocalDateTime newEndTime = request.getEndTime()
            .plusHours(request.getExtensionHoursRequested().longValue())
            .plusMinutes((long)((request.getExtensionHoursRequested() % 1) * 60));
        
        List<EquipmentRequest> conflicts = equipmentRequestRepository.findConflictingRequests(
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



// ===== SUGGESTION RESPONSE METHODS =====

@Transactional
public MessageResponse respondToSuggestion(Long requestId, EquipmentRequestRequest request, String professorEmail) {
    EquipmentRequest equipmentRequest = findRequestById(requestId);
    User professor = findUserByEmail(professorEmail);
    
    // Validate professor can respond
    if (!equipmentRequest.getUser().equals(professor)) {
        throw new IllegalArgumentException("You can only respond to suggestions on your own requests");
    }
    
    // Check if request has admin suggestion and valid status
    if (equipmentRequest.getAdminSuggestion() == null || equipmentRequest.getAdminSuggestion().trim().isEmpty()) {
        throw new IllegalArgumentException("No admin suggestion found to respond to");
    }
    
    if (equipmentRequest.getStatus() == EquipmentRequest.RequestStatus.HOD_REJECTED) {
        throw new IllegalStateException("Cannot respond to suggestions on HOD rejected requests");
    }
    
    // Set response
    equipmentRequest.setSuggestionAcknowledged(request.getSuggestionAcknowledged());
    equipmentRequest.setSuggestionResponseReason(request.getSuggestionResponseReason());
    equipmentRequest.setSuggestionResponseAt(LocalDateTime.now());
    
    equipmentRequestRepository.save(equipmentRequest);
    
    // Notify equipment admin
    String responseType = request.getSuggestionAcknowledged() ? "acknowledged" : "rejected";
    String notificationMessage = String.format("Professor %s has %s your suggestion for %s request", 
        professor.getFullName(), responseType, equipmentRequest.getEquipment().getName());
    
    if (!request.getSuggestionAcknowledged() && request.getSuggestionResponseReason() != null) {
        notificationMessage += ". Reason: " + request.getSuggestionResponseReason();
    }
    
    notificationService.addNotification(
        getEquipmentAdminEmail(),
        "Suggestion Response Received",
        notificationMessage,
        "SUGGESTION_RESPONSE"
    );
    
    return new MessageResponse("Response to suggestion submitted successfully");
}

// ===== EXTENSION REQUEST METHODS =====

@Transactional
public MessageResponse requestExtension(Long requestId, EquipmentRequestRequest request, String professorEmail) {
    EquipmentRequest equipmentRequest = findRequestById(requestId);
    User professor = findUserByEmail(professorEmail);
    
    // Validate professor can request extension
    if (!equipmentRequest.getUser().equals(professor)) {
        throw new IllegalArgumentException("You can only request extensions for your own requests");
    }
    
    // Check request status
    if (equipmentRequest.getStatus() != EquipmentRequest.RequestStatus.APPROVED && 
        equipmentRequest.getStatus() != EquipmentRequest.RequestStatus.IN_USE) {
        throw new IllegalStateException("Extensions can only be requested for approved or in-use requests");
    }
    
    // Check if already has pending extension
    if (equipmentRequest.getExtensionStatus() != null && 
        equipmentRequest.getExtensionStatus().equals("PENDING")) {
        throw new IllegalStateException("Extension request already pending");
    }
    
    // Validate daily extension limit
    double todayExtensions = getTotalExtensionHoursToday(professor.getId());
    if (todayExtensions + request.getExtensionHoursRequested() > 3.0) {
        throw new IllegalArgumentException(
            String.format("Daily extension limit exceeded. You have used %.1f hours today, requesting %.1f more exceeds the 3-hour limit", 
                todayExtensions, request.getExtensionHoursRequested()));
    }
    
    // Store original end time if first extension
    if (equipmentRequest.getOriginalEndTime() == null) {
        equipmentRequest.setOriginalEndTime(equipmentRequest.getEndTime());
    }
    
    // Set extension request details
    equipmentRequest.setExtensionReason(request.getExtensionReason());
    equipmentRequest.setExtensionStatus("PENDING");
    equipmentRequest.setExtensionRequestedAt(LocalDateTime.now());
    equipmentRequest.setExtensionHoursRequested(request.getExtensionHoursRequested());
    
    equipmentRequestRepository.save(equipmentRequest);
    
    // Check for conflicts in extended time slot
    LocalDateTime newEndTime = equipmentRequest.getEndTime().plusHours(request.getExtensionHoursRequested().longValue())
        .plusMinutes((long)((request.getExtensionHoursRequested() % 1) * 60));
    
    List<EquipmentRequest> conflicts = equipmentRequestRepository.findConflictingRequests(
        equipmentRequest.getEquipment().getId(), equipmentRequest.getEndTime(), newEndTime);
    
    // Notify equipment admin with conflict info
    String conflictInfo = conflicts.isEmpty() ? "No conflicts detected." : 
        String.format("%d conflicting booking(s) detected in extended time slot.", conflicts.size());
    
    notificationService.addNotification(
        getEquipmentAdminEmail(),
        "Extension Request Received",
        String.format("Professor %s requests %.1f hour extension for %s. %s", 
            professor.getFullName(), request.getExtensionHoursRequested(), 
            equipmentRequest.getEquipment().getName(), conflictInfo),
        "EXTENSION_REQUEST"
    );
    
    return new MessageResponse("Extension request submitted successfully");
}

@Transactional
public MessageResponse handleExtensionRequest(Long requestId, EquipmentRequestRequest request, String adminEmail) {
    EquipmentRequest equipmentRequest = findRequestById(requestId);
    User admin = findUserByEmail(adminEmail);
    
    if (!equipmentRequest.getExtensionStatus().equals("PENDING")) {
        throw new IllegalStateException("No pending extension request found");
    }
    
    equipmentRequest.setExtensionApprovedBy(admin);
    equipmentRequest.setExtensionApprovedAt(LocalDateTime.now());
    
    if (request.getApproved()) {
        equipmentRequest.setExtensionStatus("APPROVED");
        
        // Update end time
        LocalDateTime newEndTime = equipmentRequest.getEndTime()
            .plusHours(equipmentRequest.getExtensionHoursRequested().longValue())
            .plusMinutes((long)((equipmentRequest.getExtensionHoursRequested() % 1) * 60));
        equipmentRequest.setEndTime(newEndTime);
        
        // Update daily totals
        equipmentRequest.setTotalExtensionsToday(equipmentRequest.getTotalExtensionsToday() + 1);
        equipmentRequest.setTotalExtensionHoursToday(
            equipmentRequest.getTotalExtensionHoursToday() + equipmentRequest.getExtensionHoursRequested());
        
        notificationService.addNotification(
            equipmentRequest.getUser().getEmail(),
            "Extension Request Approved",
            String.format("Your %.1f hour extension for %s has been approved", 
                equipmentRequest.getExtensionHoursRequested(), equipmentRequest.getEquipment().getName()),
            "EXTENSION_APPROVED"
        );
        
        // Notify HOD if this was an escalated request
        if (equipmentRequest.isEscalatedToHod()) {
            notificationService.addNotification(
                getHodEmail(),
                "Extension Approved for Escalated Request",
                String.format("Extension approved for escalated request: %s", 
                    equipmentRequest.getEquipment().getName()),
                "EXTENSION_HOD_INFO"
            );
        }
        
    } else {
        equipmentRequest.setExtensionStatus("REJECTED");
        equipmentRequest.setRejectionReason(request.getRejectionReason());
        
        notificationService.addNotification(
            equipmentRequest.getUser().getEmail(),
            "Extension Request Rejected",
            String.format("Your extension request for %s has been rejected. Reason: %s", 
                equipmentRequest.getEquipment().getName(), 
                request.getRejectionReason() != null ? request.getRejectionReason() : "No reason provided"),
            "EXTENSION_REJECTED"
        );
    }
    
    equipmentRequestRepository.save(equipmentRequest);
    return new MessageResponse("Extension request processed successfully");
}

// ===== RETURN EQUIPMENT METHODS =====

@Transactional
public MessageResponse markEquipmentReturned(Long requestId, EquipmentRequestRequest request, String adminEmail) {
    EquipmentRequest equipmentRequest = findRequestById(requestId);
    User admin = findUserByEmail(adminEmail);
    
    // Validate request can be marked as returned
    if (equipmentRequest.getStatus() != EquipmentRequest.RequestStatus.APPROVED && 
        equipmentRequest.getStatus() != EquipmentRequest.RequestStatus.IN_USE &&
        equipmentRequest.getStatus() != EquipmentRequest.RequestStatus.HOD_APPROVED) {
        throw new IllegalStateException("Only approved or in-use requests can be marked as returned");
    }
    
    if (equipmentRequest.getReturnedAt() != null) {
        throw new IllegalStateException("Equipment already marked as returned");
    }
    
    LocalDateTime returnTime = LocalDateTime.now();
    LocalDateTime endTime = equipmentRequest.getEndTime();
    
    // Set return details
    equipmentRequest.setReturnedAt(returnTime);
    equipmentRequest.setReturnedBy(admin);
    equipmentRequest.setReturnCondition(request.getReturnCondition());
    equipmentRequest.setReturnNotes(request.getReturnNotes());
    equipmentRequest.setIsEarlyReturn(returnTime.isBefore(endTime));
    equipmentRequest.setIsEarlyReturn(returnTime.isAfter(endTime));
    equipmentRequest.setStatus(EquipmentRequest.RequestStatus.RETURNED);
    
    equipmentRequestRepository.save(equipmentRequest);
    
    // Release equipment
    equipmentService.releaseEquipment(
        equipmentRequest.getEquipment().getId(), 
        equipmentRequest.getRequestedQuantity()
    );
    
    // Notify professor
    String returnType = equipmentRequest.getIsEarlyReturn() ? "early" : 
                       equipmentRequest.getIsLateReturn() ? "late" : "on time";
    
    notificationService.addNotification(
        equipmentRequest.getUser().getEmail(),
        "Equipment Returned",
        String.format("Your equipment %s has been marked as returned (%s) - Condition: %s", 
            equipmentRequest.getEquipment().getName(), returnType, request.getReturnCondition()),
        "EQUIPMENT_RETURNED"
    );
    
    return new MessageResponse("Equipment marked as returned successfully");
}

// ===== HELPER METHODS =====

public List<EquipmentRequestResponse> getActiveRequests() {
    return equipmentRequestRepository.findActiveRequests().stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
}

public List<EquipmentRequestResponse> getExtensionRequests() {
    return equipmentRequestRepository.findPendingExtensionRequests().stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
}

private double getTotalExtensionHoursToday(Long userId) {
    LocalDateTime startOfDay = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0);
    LocalDateTime endOfDay = startOfDay.plusDays(1).minusNanos(1);
    
    return equipmentRequestRepository.getTotalExtensionHoursForUserToday(userId, startOfDay, endOfDay);
}

// Auto-complete returned requests
@Transactional
public void markReturnedRequestsAsCompleted() {
    List<EquipmentRequest> returnedRequests = equipmentRequestRepository
        .findByStatusAndReturnedAtIsNotNull(EquipmentRequest.RequestStatus.RETURNED);
    
    for (EquipmentRequest request : returnedRequests) {
        request.setStatus(EquipmentRequest.RequestStatus.COMPLETED);
        equipmentRequestRepository.save(request);
    }
}

private void updateRequestStatusIfNeeded(EquipmentRequest request) {
    LocalDateTime now = LocalDateTime.now();
    
    // Auto-update APPROVED to IN_USE when start time reached
    if (request.getStatus() == EquipmentRequest.RequestStatus.APPROVED && 
        request.getStartTime().isBefore(now)) {
        request.setStatus(EquipmentRequest.RequestStatus.IN_USE);
        equipmentRequestRepository.save(request);
    }
    
    // Mark as late return if overdue
    if (request.getStatus() == EquipmentRequest.RequestStatus.IN_USE && 
        request.getEndTime().isBefore(now) && 
        request.getReturnedAt() == null && 
        (request.getIsLateReturn() == null || !request.getIsLateReturn())) {
        request.setIsLateReturn(true);
        equipmentRequestRepository.save(request);
    }
}


}