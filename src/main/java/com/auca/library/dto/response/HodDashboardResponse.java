package com.auca.library.dto.response;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class HodDashboardResponse {

    // Pending approvals
    private int pendingProfessorApprovals;
    private int pendingCourseApprovalCount; 
    private int escalatedEquipmentRequests;

    // Professor statistics
    private int totalProfessors;
    private int approvedProfessors;
    private int activeProfessors; 

    // Course statistics
    private int totalCourses;
    private int coursesWithProfessors;
    private int mostPopularCourseRequests;

    // Equipment oversight
    private int totalEquipmentRequests;
    private int escalationRate; // Percentage of requests that get escalated
    private List<String> mostEscalatedEquipment;

    // Recent activity requiring attention
    private List<ProfessorResponse> pendingProfessors;
    private List<EquipmentRequestResponse> escalatedRequests;
    private List<ProfessorCourseApprovalItem> pendingCourseApprovalItems; 

    @Getter
    @Setter
    public static class ProfessorCourseApprovalItem {
        private Long professorId;
        private String professorName;
        private String professorEmail;
        private List<String> requestedCourses;
        private LocalDateTime requestedAt; 
}

}
