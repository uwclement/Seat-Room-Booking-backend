package com.auca.library.dto.response;

import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class EquipmentAvailabilityResponse {
    private Long equipmentId;
    private String equipmentName;
    private Integer totalQuantity;
    private Integer availableQuantity;
    private Integer reservedQuantity;
    
    // Current reservations
    private List<EquipmentReservation> currentReservations;
    
    // Availability forecast
    private List<AvailabilitySlot> nextAvailableSlots;
    
    @Getter
    @Setter
    public static class EquipmentReservation {
        private Long requestId;
        private String userName;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private Integer quantity;
        private String courseName;
        private String status;
    }
    
    @Getter
    @Setter
    public static class AvailabilitySlot {
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private Integer availableQuantity;
        private boolean fullyAvailable;
    }
}