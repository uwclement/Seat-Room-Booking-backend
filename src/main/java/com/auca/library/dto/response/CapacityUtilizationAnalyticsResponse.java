package com.auca.library.dto.response;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CapacityUtilizationAnalyticsResponse {
    private Long totalBookings;
    private Long underCapacityBookings;
    private Long overCapacityBookings;
    private Long optimalCapacityBookings;
    private Double capacityUtilizationRate;
    private Double averageCapacityUsage;
    private Integer averageParticipantsPerBooking;
    private List<RoomCapacityDetail> roomCapacityDetails;
    private String mostUnderutilizedRoom;
    private String mostOverutilizedRoom;
}

@Getter
@Setter
class RoomCapacityDetail {
    private String roomName;
    private Integer roomCapacity;
    private Double averageUtilization;
    private Long totalBookings;
    private Long underCapacityBookings;
}