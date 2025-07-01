package com.auca.library.service;

import com.auca.library.dto.response.EquipmentUsageStatsResponse;
import com.auca.library.model.*;
import com.auca.library.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AnalyticsService {

    @Autowired
    private EquipmentRepository equipmentRepository;
    
    @Autowired
    private EquipmentRequestRepository equipmentRequestRepository;
    
    @Autowired
    private UserRepository userRepository;

    public List<EquipmentUsageStatsResponse> getEquipmentUsageStats(LocalDateTime startDate, LocalDateTime endDate) {
        List<Equipment> allEquipment = equipmentRepository.findAll();
        
        return allEquipment.stream()
                .map(equipment -> {
                    EquipmentUsageStatsResponse stats = new EquipmentUsageStatsResponse();
                    stats.setEquipmentId(equipment.getId());
                    stats.setEquipmentName(equipment.getName());
                    
                    // Get all requests for this equipment in date range
                    List<EquipmentRequest> requests = equipmentRequestRepository.findAll().stream()
                            .filter(req -> req.getEquipment().equals(equipment))
                            .filter(req -> req.getCreatedAt().isAfter(startDate) && req.getCreatedAt().isBefore(endDate))
                            .collect(Collectors.toList());
                    
                    stats.setTotalRequests(requests.size());
                    stats.setApprovedRequests((int) requests.stream()
                            .filter(req -> req.getStatus() == EquipmentRequest.RequestStatus.APPROVED ||
                                          req.getStatus() == EquipmentRequest.RequestStatus.HOD_APPROVED)
                            .count());
                    stats.setRejectedRequests((int) requests.stream()
                            .filter(req -> req.getStatus() == EquipmentRequest.RequestStatus.REJECTED ||
                                          req.getStatus() == EquipmentRequest.RequestStatus.HOD_REJECTED)
                            .count());
                    stats.setPendingRequests((int) requests.stream()
                            .filter(req -> req.getStatus() == EquipmentRequest.RequestStatus.PENDING ||
                                          req.getStatus() == EquipmentRequest.RequestStatus.ESCALATED)
                            .count());
                    
                    // Time-based stats
                    LocalDateTime oneMonthAgo = LocalDateTime.now().minusMonths(1);
                    LocalDateTime oneWeekAgo = LocalDateTime.now().minusWeeks(1);
                    
                    stats.setRequestsThisMonth((int) requests.stream()
                            .filter(req -> req.getCreatedAt().isAfter(oneMonthAgo))
                            .count());
                    stats.setRequestsThisWeek((int) requests.stream()
                            .filter(req -> req.getCreatedAt().isAfter(oneWeekAgo))
                            .count());
                    
                    // Last request and approval dates
                    requests.stream()
                            .map(EquipmentRequest::getCreatedAt)
                            .max(LocalDateTime::compareTo)
                            .ifPresent(stats::setLastRequestDate);
                    
                    requests.stream()
                            .filter(req -> req.getApprovedAt() != null)
                            .map(EquipmentRequest::getApprovedAt)
                            .max(LocalDateTime::compareTo)
                            .ifPresent(stats::setLastApprovalDate);
                    
                    // User statistics
                    stats.setUniqueUsers((int) requests.stream()
                            .map(EquipmentRequest::getUser)
                            .distinct()
                            .count());
                    
                    stats.setStudentRequests((int) requests.stream()
                            .filter(req -> hasRole(req.getUser(), "ROLE_USER"))
                            .count());
                    stats.setProfessorRequests((int) requests.stream()
                            .filter(req -> hasRole(req.getUser(), "ROLE_PROFESSOR"))
                            .count());
                    
                    return stats;
                })
                .collect(Collectors.toList());
    }

    public Map<String, Object> getOverallSystemStats() {
        Map<String, Object> stats = new java.util.HashMap<>();
        
        // Equipment stats
        stats.put("totalEquipment", equipmentRepository.count());
        stats.put("availableEquipment", equipmentRepository.countAvailableEquipment());
        stats.put("studentAllowedEquipment", equipmentRepository.countStudentAllowedEquipment());
        stats.put("lowInventoryEquipment", equipmentRepository.findLowInventoryEquipment(2).size());
        
        // Request stats
        List<EquipmentRequest> allRequests = equipmentRequestRepository.findAll();
        stats.put("totalRequests", allRequests.size());
        stats.put("pendingRequests", equipmentRequestRepository.findPendingRequests().size());
        stats.put("escalatedRequests", equipmentRequestRepository.findEscalatedRequests().size());
        
        // User stats
        stats.put("totalProfessors", userRepository.findApprovedProfessors().size());
        stats.put("pendingProfessors", userRepository.findPendingProfessors().size());
        
        // Recent activity (last 30 days)
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        long recentRequests = allRequests.stream()
                .filter(req -> req.getCreatedAt().isAfter(thirtyDaysAgo))
                .count();
        stats.put("recentRequests", recentRequests);
        
        return stats;
    }

    private boolean hasRole(User user, String roleName) {
        return user.getRoles().stream()
                .anyMatch(role -> role.getName().name().equals(roleName));
    }
}