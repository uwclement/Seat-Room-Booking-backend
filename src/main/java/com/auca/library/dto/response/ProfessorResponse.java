package com.auca.library.dto.response;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

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
    private List<CourseResponse> assignedCourses = new ArrayList<>();
    private String employeeId;
    
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
    public List<CourseResponse> getAssignedCourses() {
    return assignedCourses;
}

public void setAssignedCourses(List<CourseResponse> assignedCourses) {
    this.assignedCourses = assignedCourses;
}

public String getEmployeeId() {
    return employeeId;
}

public void setEmployeeId(String employeeId) {
    this.employeeId = employeeId;
}
}