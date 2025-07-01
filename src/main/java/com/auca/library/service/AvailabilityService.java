package com.auca.library.service;

import com.auca.library.dto.response.EquipmentAvailabilityResponse;
import com.auca.library.dto.response.LabClassAvailabilityResponse;
import com.auca.library.model.*;
import com.auca.library.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AvailabilityService {

    @Autowired
    private EquipmentRepository equipmentRepository;
    
    @Autowired
    private LabClassRepository labClassRepository;
    
    @Autowired
    private EquipmentRequestRepository equipmentRequestRepository;

    public EquipmentAvailabilityResponse getEquipmentAvailability(Long equipmentId, LocalDateTime startTime, LocalDateTime endTime) {
        Equipment equipment = equipmentRepository.findById(equipmentId)
                .orElseThrow(() -> new RuntimeException("Equipment not found"));
        
        EquipmentAvailabilityResponse response = new EquipmentAvailabilityResponse();
        response.setEquipmentId(equipment.getId());
        response.setEquipmentName(equipment.getName());
        response.setTotalQuantity(equipment.getQuantity());
        response.setAvailableQuantity(equipment.getAvailableQuantity());
        response.setReservedQuantity(equipment.getQuantity() - equipment.getAvailableQuantity());
        
        // Get conflicting requests in the time period
        List<EquipmentRequest> conflicts = equipmentRequestRepository.findConflictingRequests(
            equipmentId, startTime, endTime);
        
        response.setCurrentReservations(conflicts.stream()
                .map(req -> {
                    EquipmentAvailabilityResponse.EquipmentReservation reservation = 
                        new EquipmentAvailabilityResponse.EquipmentReservation();
                    reservation.setRequestId(req.getId());
                    reservation.setUserName(req.getUser().getFullName());
                    reservation.setStartTime(req.getStartTime());
                    reservation.setEndTime(req.getEndTime());
                    reservation.setQuantity(req.getRequestedQuantity());
                    reservation.setStatus(req.getStatus().name());
                    if (req.getCourse() != null) {
                        reservation.setCourseName(req.getCourse().getCourseName());
                    }
                    return reservation;
                })
                .collect(Collectors.toList()));
        
        return response;
    }

    public LabClassAvailabilityResponse getLabClassAvailability(Long labClassId, LocalDateTime startTime, LocalDateTime endTime) {
        LabClass labClass = labClassRepository.findById(labClassId)
                .orElseThrow(() -> new RuntimeException("Lab class not found"));
        
        LabClassAvailabilityResponse response = new LabClassAvailabilityResponse();
        response.setLabClassId(labClass.getId());
        response.setLabNumber(labClass.getLabNumber());
        response.setLabName(labClass.getName());
        response.setAvailable(labClass.isAvailable());
        
        // Check if lab is available in the requested time slot
        boolean isAvailable = labClassRepository.isLabAvailable(labClassId, startTime, endTime);
        response.setCurrentlyBooked(!isAvailable);
        
        // Get upcoming bookings for this lab
        List<EquipmentRequest> labBookings = equipmentRequestRepository.findByLabClassId(labClassId);
        
        response.setUpcomingBookings(labBookings.stream()
                .filter(req -> req.getStartTime().isAfter(LocalDateTime.now()))
                .map(req -> {
                    LabClassAvailabilityResponse.LabBookingSlot slot = 
                        new LabClassAvailabilityResponse.LabBookingSlot();
                    slot.setStartTime(req.getStartTime());
                    slot.setEndTime(req.getEndTime());
                    slot.setProfessorName(req.getUser().getFullName());
                    if (req.getCourse() != null) {
                        slot.setCourseName(req.getCourse().getCourseName());
                    }
                    return slot;
                })
                .collect(Collectors.toList()));
        
        return response;
    }
}
