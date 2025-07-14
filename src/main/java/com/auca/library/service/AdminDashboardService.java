package com.auca.library.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.auca.library.dto.response.AdminDashboardResponse;
import com.auca.library.repository.UserRepository;

@Service
public class AdminDashboardService {

    @Autowired
    private UserRepository userRepository;

    public AdminDashboardResponse getDashboardStats() {
        AdminDashboardResponse response = new AdminDashboardResponse();
        
        response.setTotalUsers(userRepository.count());
        response.setTotalStudents(userRepository.findAllStudents().size());
        response.setTotalStaff(userRepository.findAllStaff().size());
        response.setTotalAdmins(userRepository.findAllAdmins().size());
        response.setTotalLibrarians(userRepository.findAllLibrarians().size());
        response.setTotalProfessors(userRepository.findApprovedProfessors().size());
        response.setPendingProfessors(userRepository.findPendingProfessors().size());
        response.setUsersWithDefaultPasswords(userRepository.findStaffWithDefaultPasswords().size());
        response.setActiveUsers(userRepository.findActiveUsers().size());
        response.setInactiveUsers((int) (userRepository.count() - userRepository.findActiveUsers().size()));

        return response;
    }

    public Map<String, Integer> getUserStatsByRole() {
        Map<String, Integer> stats = new HashMap<>();
        stats.put("STUDENTS", userRepository.findAllStudents().size());
        stats.put("ADMINS", userRepository.findAllAdmins().size());
        stats.put("LIBRARIANS", userRepository.findAllLibrarians().size());
        stats.put("PROFESSORS", userRepository.findApprovedProfessors().size());
        stats.put("PENDING_PROFESSORS", userRepository.findPendingProfessors().size());
        
        // Add other roles as needed
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
        
        // Get all users and group by location
        userRepository.findAll().stream()
                .collect(Collectors.groupingBy(
                        user -> user.getLocation() != null ? user.getLocation().name() : "UNKNOWN",
                        Collectors.collectingAndThen(Collectors.counting(), Math::toIntExact)))
                .forEach(stats::put);
        
        return stats;
    }

    public Map<String, Integer> getRegistrationTrends(int days) {
        Map<String, Integer> trends = new HashMap<>();
        LocalDateTime startDate = LocalDateTime.now().minusDays(days);
        
        // This is a simplified version - you might want to implement actual date-based queries
        // For now, returning sample data
        for (int i = days; i >= 0; i--) {
            LocalDate date = LocalDate.now().minusDays(i);
            trends.put(date.toString(), (int) (Math.random() * 10)); // Replace with actual data
        }
        
        return trends;
    }
}