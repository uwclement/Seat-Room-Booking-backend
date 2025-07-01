package com.auca.library.service;

import com.auca.library.dto.response.*;
import com.auca.library.model.*;
import com.auca.library.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class DashboardService {

    @Autowired
    private EquipmentRepository equipmentRepository;
    
    @Autowired
    private LabClassRepository labClassRepository;
    
    @Autowired
    private CourseRepository courseRepository;
    
    @Autowired
    private EquipmentRequestRepository equipmentRequestRepository;
    
    @Autowired
    private UserRepository userRepository;

    public ProfessorDashboardResponse getProfessorDashboard(String professorEmail) {
        User professor = findUserByEmail(professorEmail);
        ProfessorDashboardResponse dashboard = new ProfessorDashboardResponse();
        
        // Basic info
        dashboard.setFullName(professor.getFullName());
        dashboard.setProfessorApproved(professor.isProfessorApproved());
        dashboard.setApprovedCourseCount(professor.getApprovedCourses().size());
        
        // Request statistics
        List<EquipmentRequest> allRequests = equipmentRequestRepository.findByUserOrderByCreatedAtDesc(professor);
        dashboard.setTotalRequests(allRequests.size());
        dashboard.setPendingRequests((int) allRequests.stream()
                .filter(req -> req.getStatus() == EquipmentRequest.RequestStatus.PENDING ||
                              req.getStatus() == EquipmentRequest.RequestStatus.ESCALATED)
                .count());
        dashboard.setApprovedRequests((int) allRequests.stream()
                .filter(req -> req.getStatus() == EquipmentRequest.RequestStatus.APPROVED ||
                              req.getStatus() == EquipmentRequest.RequestStatus.HOD_APPROVED)
                .count());
        
        // Active bookings (currently using equipment)
        LocalDateTime now = LocalDateTime.now();
        dashboard.setActiveBookings((int) allRequests.stream()
                .filter(req -> (req.getStatus() == EquipmentRequest.RequestStatus.APPROVED ||
                               req.getStatus() == EquipmentRequest.RequestStatus.HOD_APPROVED) &&
                               req.getStartTime().isBefore(now) && req.getEndTime().isAfter(now))
                .count());
        
        // Recent requests (last 5)
        dashboard.setRecentRequests(allRequests.stream()
                .limit(5)
                .map(this::mapToEquipmentRequestResponse)
                .collect(Collectors.toList()));
        
        // Available resources
        dashboard.setAvailableEquipmentCount(equipmentRepository.findByAvailableTrue().size());
        dashboard.setAvailableLabClassCount(labClassRepository.findByAvailableTrue().size());
        
        // Alerts
        dashboard.setHasRejectedRequests(allRequests.stream()
                .anyMatch(req -> req.getStatus() == EquipmentRequest.RequestStatus.REJECTED));
        dashboard.setEscalatableRequestCount((int) allRequests.stream()
                .filter(req -> req.getStatus() == EquipmentRequest.RequestStatus.REJECTED)
                .count());
        
        return dashboard;
    }

    public EquipmentAdminDashboardResponse getEquipmentAdminDashboard() {
        EquipmentAdminDashboardResponse dashboard = new EquipmentAdminDashboardResponse();
        
        // Basic counts
        dashboard.setTotalEquipment((int) equipmentRepository.count());
        dashboard.setAvailableEquipment(equipmentRepository.findByAvailableTrue().size());
        dashboard.setTotalLabClasses((int) labClassRepository.count());
        dashboard.setAvailableLabClasses(labClassRepository.findByAvailableTrue().size());
        dashboard.setTotalCourses((int) courseRepository.count());
        dashboard.setActiveCourses(courseRepository.findByActiveTrue().size());
        
        // Request statistics
        List<EquipmentRequest> allRequests = equipmentRequestRepository.findAll();
        dashboard.setPendingRequests(equipmentRequestRepository.findPendingRequests().size());
        
        LocalDateTime today = LocalDateTime.now().toLocalDate().atStartOfDay();
        LocalDateTime weekStart = today.minusDays(7);
        LocalDateTime monthStart = today.minusDays(30);
        
        dashboard.setTodaysRequests((int) allRequests.stream()
                .filter(req -> req.getCreatedAt().isAfter(today))
                .count());
        dashboard.setThisWeekRequests((int) allRequests.stream()
                .filter(req -> req.getCreatedAt().isAfter(weekStart))
                .count());
        dashboard.setThisMonthRequests((int) allRequests.stream()
                .filter(req -> req.getCreatedAt().isAfter(monthStart))
                .count());
        
        // Recent requests
        dashboard.setRecentRequests(equipmentRequestRepository.findPendingRequests().stream()
                .limit(10)
                .map(this::mapToEquipmentRequestResponse)
                .collect(Collectors.toList()));
        
        // Alerts
        dashboard.setLowInventoryAlerts(equipmentRepository.findLowInventoryEquipment(2).size());
        dashboard.setOverdueApprovals((int) allRequests.stream()
                .filter(req -> req.getStatus() == EquipmentRequest.RequestStatus.PENDING &&
                              req.getCreatedAt().isBefore(LocalDateTime.now().minusHours(24)))
                .count());
        dashboard.setHasHodEscalations(!equipmentRequestRepository.findEscalatedRequests().isEmpty());
        
        return dashboard;
    }

    public HodDashboardResponse getHodDashboard() {
        HodDashboardResponse dashboard = new HodDashboardResponse();
        
        // Pending approvals
        dashboard.setPendingProfessorApprovals(userRepository.findPendingProfessors().size());
        dashboard.setEscalatedEquipmentRequests(equipmentRequestRepository.findEscalatedRequests().size());
        
        // Professor statistics
        List<User> allProfessors = userRepository.findApprovedProfessors();
        dashboard.setTotalProfessors(allProfessors.size());
        dashboard.setApprovedProfessors(allProfessors.size());
        
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        dashboard.setActiveProfessors(userRepository.findActiveProfessors(thirtyDaysAgo).size());
        
        // Course statistics
        dashboard.setTotalCourses((int) courseRepository.count());
        dashboard.setCoursesWithProfessors((int) courseRepository.findAll().stream()
                .filter(course -> !course.getProfessors().isEmpty())
                .count());
        
        // Equipment oversight
        List<EquipmentRequest> allRequests = equipmentRequestRepository.findAll();
        dashboard.setTotalEquipmentRequests(allRequests.size());
        
        long totalRequests = allRequests.size();
        long escalatedCount = allRequests.stream()
                .filter(req -> req.isEscalatedToHod())
                .count();
        dashboard.setEscalationRate(totalRequests > 0 ? (int)((escalatedCount * 100) / totalRequests) : 0);
        
        // Recent activity requiring attention
        dashboard.setPendingProfessors(userRepository.findPendingProfessors().stream()
                .map(this::mapToProfessorResponse)
                .collect(Collectors.toList()));
        dashboard.setEscalatedRequests(equipmentRequestRepository.findEscalatedRequests().stream()
                .map(this::mapToEquipmentRequestResponse)
                .collect(Collectors.toList()));
        
        return dashboard;
    }

    // Helper methods
    private User findUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found: " + email));
    }

    private EquipmentRequestResponse mapToEquipmentRequestResponse(EquipmentRequest request) {
        // Implementation would be similar to the one in EquipmentRequestService
        EquipmentRequestResponse response = new EquipmentRequestResponse();
        response.setId(request.getId());
        response.setEquipmentId(request.getEquipment().getId());
        response.setEquipmentName(request.getEquipment().getName());
        response.setReason(request.getReason());
        response.setStatus(request.getStatus());
        response.setCreatedAt(request.getCreatedAt());
        response.setUserFullName(request.getUser().getFullName());
        // ... set other fields
        return response;
    }

    private ProfessorResponse mapToProfessorResponse(User professor) {
        ProfessorResponse response = new ProfessorResponse();
        response.setId(professor.getId());
        response.setFullName(professor.getFullName());
        response.setEmail(professor.getEmail());
        response.setProfessorApproved(professor.isProfessorApproved());
        // ... set other fields
        return response;
    }
}