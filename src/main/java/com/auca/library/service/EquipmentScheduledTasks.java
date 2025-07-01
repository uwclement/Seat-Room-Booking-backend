package com.auca.library.service;

import com.auca.library.model.EquipmentRequest;
import com.auca.library.repository.EquipmentRequestRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class EquipmentScheduledTasks {

    @Autowired
    private EquipmentRequestRepository equipmentRequestRepository;
    
    @Autowired
    private EquipmentRequestService equipmentRequestService;

    // Complete equipment requests that have ended
    @Scheduled(fixedRate = 300000) // Every 5 minutes
    @Transactional
    public void completeExpiredEquipmentRequests() {
        LocalDateTime now = LocalDateTime.now();
        
        List<EquipmentRequest> expiredRequests = equipmentRequestRepository.findAll().stream()
                .filter(req -> (req.getStatus() == EquipmentRequest.RequestStatus.APPROVED || 
                               req.getStatus() == EquipmentRequest.RequestStatus.HOD_APPROVED) &&
                               req.getEndTime().isBefore(now))
                .toList();
        
        for (EquipmentRequest request : expiredRequests) {
            equipmentRequestService.completeRequest(request.getId());
        }
    }
}