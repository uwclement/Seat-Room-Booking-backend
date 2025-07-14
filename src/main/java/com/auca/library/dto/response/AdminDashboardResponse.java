package com.auca.library.dto.response;

import lombok.Data;

@Data
public class AdminDashboardResponse {
    private long totalUsers;
    private long totalStudents;
    private long totalStaff;
    private long totalAdmins;
    private long totalLibrarians;
    private long totalProfessors;
    private long pendingProfessors;
    private long usersWithDefaultPasswords;
    private long activeUsers;
    private long inactiveUsers;
}