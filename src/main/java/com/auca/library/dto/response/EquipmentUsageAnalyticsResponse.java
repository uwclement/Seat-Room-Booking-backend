package com.auca.library.dto.response;

import java.util.List;
import java.util.Map;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EquipmentUsageAnalyticsResponse {
    private Long totalEquipmentRequests;
    private Integer uniqueEquipmentTypes;
    private String mostRequestedEquipment;
    private Map<String, Long> equipmentRequestCounts;
    private List<EquipmentUsageDetail> equipmentDetails;
    private Double averageEquipmentPerBooking;
    private Long totalApprovedRequests;
    private Long totalRejectedRequests;
    private Double approvalRate;
}

@Getter
@Setter
class EquipmentUsageDetail {
    private String equipmentName;
    private Long requestCount;
    private Long approvedCount;
    private Long rejectedCount;
    private Double approvalRate;
}
