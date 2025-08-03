package com.auca.library.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.auca.library.dto.request.EquipmentRequestApprovalRequest;
import com.auca.library.dto.request.LabRequestRequest;
import com.auca.library.dto.response.LabRequestResponse;
import com.auca.library.dto.response.MessageResponse;
import com.auca.library.exception.ResourceNotFoundException;
import com.auca.library.model.Course;
import com.auca.library.model.LabClass;
import com.auca.library.model.LabRequest;
import com.auca.library.model.User;
import com.auca.library.repository.CourseRepository;
import com.auca.library.repository.LabClassRepository;
import com.auca.library.repository.LabClassRequestRepository;
import com.auca.library.repository.UserRepository;

@Service
public class LabClassRequestService {

    @Autowired
    private LabClassRequestRepository labClassRequestRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private LabClassRepository labClassRepository;
    
    @Autowired
    private CourseRepository courseRepository;
    
    @Autowired
    private NotificationService notificationService;

    @Transactional
    public LabRequestResponse createLabClassRequest(LabRequestRequest request, String userEmail) {
        User user = findUserByEmail(userEmail);
        LabClass labClass = findLabClassById(request.getLabClassId());
        Course course = findCourseById(request.getCourseId());
        
        // Validate professor can teach this course
        if (!user.getApprovedCourses().contains(course)) {
            throw new IllegalArgumentException("You are not approved to teach this course");
        }
        
        // Validate time
        if (request.getStartTime().isAfter(request.getEndTime())) {
            throw new IllegalArgumentException("Start time must be before end time");
        }
        
        // Check lab availability
        if (!labClassRepository.isLabAvailableForBooking(labClass.getId(), request.getStartTime(), request.getEndTime())) {
            throw new IllegalArgumentException("Lab is not available for the requested time slot");
        }
        
        // Create request
        LabRequest labRequest = new LabRequest();
        labRequest.setUser(user);
        labRequest.setLabClass(labClass);
        labRequest.setCourse(course);
        labRequest.setReason(request.getReason());
        labRequest.setStartTime(request.getStartTime());
        labRequest.setEndTime(request.getEndTime());
        
        labRequest = labClassRequestRepository.save(labRequest);
        
        // Notify admin
        notificationService.addNotification(
            getEquipmentAdminEmail(),
            "New Lab Class Request",
            String.format("New lab request for %s by %s", labClass.getName(), user.getFullName()),
            "LAB_REQUEST"
        );
        
        return mapToResponse(labRequest);
    }

    @Transactional
    public MessageResponse handleLabRequestApproval(Long requestId, EquipmentRequestApprovalRequest request, String adminEmail) {
        LabRequest labRequest = findRequestById(requestId);
        User admin = findUserByEmail(adminEmail);
        
        if (labRequest.getStatus() != LabRequest.RequestStatus.PENDING) {
            throw new IllegalStateException("Only pending requests can be processed");
        }
        
        labRequest.setApprovedBy(admin);
        labRequest.setApprovedAt(LocalDateTime.now());
        
        if (request.isApproved()) {
            labRequest.setStatus(LabRequest.RequestStatus.APPROVED);
            
            notificationService.addNotification(
                labRequest.getUser().getEmail(),
                "Lab Request Approved",
                String.format("Your lab request for %s has been approved", 
                    labRequest.getLabClass().getName()),
                "LAB_APPROVED"
            );
        } else {
            labRequest.setStatus(LabRequest.RequestStatus.REJECTED);
            labRequest.setRejectionReason(request.getRejectionReason());
            labRequest.setAdminSuggestion(request.getAdminSuggestion());
            
            notificationService.addNotification(
                labRequest.getUser().getEmail(),
                "Lab Request Rejected",
                String.format("Your lab request for %s has been rejected. Reason: %s", 
                    labRequest.getLabClass().getName(), request.getRejectionReason()),
                "LAB_REJECTED"
            );
        }
        
        labClassRequestRepository.save(labRequest);
        return new MessageResponse("Lab request processed successfully");
    }

    public List<LabRequestResponse> getCurrentUserRequests(String userEmail) {
        User user = findUserByEmail(userEmail);
        return labClassRequestRepository.findByUserOrderByCreatedAtDesc(user).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<LabRequestResponse> getPendingRequests() {
        return labClassRequestRepository.findPendingRequests().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // Get all Lab requests for the current month (Equipment Admin)
    public List<LabRequestResponse> getCurrentMonthRequests() {
    LocalDateTime startOfMonth = LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
    LocalDateTime endOfMonth = startOfMonth.plusMonths(1).minusNanos(1);
    
    return labClassRequestRepository.findRequestsInDateRange(startOfMonth, endOfMonth).stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
    }


     // Cancel request
@Transactional
public MessageResponse cancelRequest(Long requestId, String userEmail) {
    LabRequest labRequest = findRequestById(requestId);
    User user = findUserByEmail(userEmail);
    
    if (!labRequest.getUser().equals(user)) {
        throw new IllegalArgumentException("You can only cancel your own requests");
    }
    
    if (labRequest.getStatus() != LabRequest.RequestStatus.PENDING) {
        throw new IllegalStateException("Only pending requests can be cancelled");
    }
    
    labRequest.setStatus(LabRequest.RequestStatus.CANCELLED);
    labClassRequestRepository.save(labRequest);
    
    // Release reserved equipment
    // equipmentService.releaseEquipment(labRequest.getEquipment().getId(), labRequest.getRequestedQuantity());
    
    return new MessageResponse("Request cancelled successfully");
}


    // Helper methods
    private User findUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + email));
    }

    private LabClass findLabClassById(Long id) {
        return labClassRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Lab class not found: " + id));
    }

    private Course findCourseById(Long id) {
        return courseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found: " + id));
    }

    private LabRequest findRequestById(Long id) {
        return labClassRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Lab request not found: " + id));
    }

    private String getEquipmentAdminEmail() {
        return userRepository.findEquipmentAdmin()
                .map(User::getEmail)
                .orElse(null);
    }

    

    private LabRequestResponse mapToResponse(LabRequest request) {
        LabRequestResponse response = new LabRequestResponse();
        response.setId(request.getId());
        response.setLabClassId(request.getLabClass().getId());
        response.setLabClassName(request.getLabClass().getName());
        response.setLabNumber(request.getLabClass().getLabNumber());
        response.setCourseId(request.getCourse().getId());
        response.setCourseCode(request.getCourse().getCourseCode());
        response.setCourseName(request.getCourse().getCourseName());
        response.setReason(request.getReason());
        response.setStartTime(request.getStartTime());
        response.setEndTime(request.getEndTime());
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
        }
        
        if (request.getApprovedBy() != null) {
            response.setApprovedByName(request.getApprovedBy().getFullName());
        }
        
        return response;
    }
}