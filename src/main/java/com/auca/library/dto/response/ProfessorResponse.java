package com.auca.library.dto.response;

import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class ProfessorResponse {
    private Long id;
    private String fullName;
    private String email;
    private String studentId;
    
    // Professor status
    private boolean professorApproved;
    private LocalDateTime professorApprovedAt;
    private String approvedByHodName;
    
    // Course associations
    private List<Long> courseIds;
    private List<String> courseNames;
    private List<String> courseCodes;
    private int approvedCourseCount;
    
    // Statistics
    private int totalEquipmentRequests;
    private int approvedEquipmentRequests;
    private int rejectedEquipmentRequests;
    private int pendingEquipmentRequests;
    
    // Recent activity
    private LocalDateTime lastRequestDate;
    private boolean hasRecentActivity; // Activity in last 30 days

    private List<Long> pendingCourseIds;
    private List<String> pendingCourseNames;

    public List<Long> getPendingCourseIds() { return pendingCourseIds; }
    public void setPendingCourseIds(List<Long> pendingCourseIds) { this.pendingCourseIds = pendingCourseIds; }

    public List<String> getPendingCourseNames() { return pendingCourseNames; } 
    public void setPendingCourseNames(List<String> pendingCourseNames) { this.pendingCourseNames = pendingCourseNames; }
    
}