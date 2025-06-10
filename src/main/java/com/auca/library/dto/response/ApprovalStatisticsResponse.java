package com.auca.library.dto.response;

import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class ApprovalStatisticsResponse {
    private Long totalRequiringApproval;
    private Long approvedCount;
    private Long rejectedCount;
    private Long pendingCount;
    private Double approvalRate;
    private Double rejectionRate;
    private Double averageApprovalTimeHours;
    private List<ApprovalTrendData> approvalTrends;
    private List<String> topRejectionReasons;
    private String fastestApprovingAdmin;
    private String mostActiveApprovingAdmin;
}

@Getter
@Setter
class ApprovalTrendData {
    private LocalDateTime date;
    private Long approvals;
    private Long rejections;
    private Long pending;
}
