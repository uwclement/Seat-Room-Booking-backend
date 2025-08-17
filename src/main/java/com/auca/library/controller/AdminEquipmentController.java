package com.auca.library.controller;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.auca.library.dto.request.EquipmentRequest;
import com.auca.library.dto.request.EquipmentRequestApprovalRequest;
import com.auca.library.dto.request.EquipmentStatusUpdateRequest;
import com.auca.library.dto.response.EquipmentResponse;
import com.auca.library.dto.response.EquipmentUnitResponse;
import com.auca.library.dto.response.MessageResponse;
import com.auca.library.model.EquipmentLog;
import com.auca.library.model.User;
import com.auca.library.service.EquipmentRequestService;
import com.auca.library.service.EquipmentService;
import com.auca.library.service.EquipmentUnitService;
import com.auca.library.service.UserService;

import jakarta.validation.Valid;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/equipment-admin/equipment")
@PreAuthorize("hasRole('EQUIPMENT_ADMIN') or hasRole('ADMIN')")
public class AdminEquipmentController {

    @Autowired
    private EquipmentService equipmentService;

    @Autowired
    private UserService userService;

    @Autowired
    private EquipmentUnitService equipmentUnitService;

    @Autowired
    private EquipmentRequestService equipmentRequestService;

    // Helper method to get current user
     private User getCurrentUser(UserDetails userDetails) {
        return userService.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found: " + userDetails.getUsername()));
    }

    // Location-based endpoints
    @GetMapping
    public ResponseEntity<List<EquipmentResponse>> getAllEquipment(@AuthenticationPrincipal UserDetails userDetails) {
        User user = getCurrentUser(userDetails);
        List<EquipmentResponse> equipment = equipmentService.getAllEquipmentByLocation(user.getLocation());
        return ResponseEntity.ok(equipment);
    }

    @GetMapping("/available")
    public ResponseEntity<List<EquipmentResponse>> getAvailableEquipment(@AuthenticationPrincipal UserDetails userDetails) {
        User user = getCurrentUser(userDetails);
        List<EquipmentResponse> equipment = equipmentService.getAvailableEquipmentByLocation(user.getLocation());
        return ResponseEntity.ok(equipment);
    }

    @GetMapping("/student-allowed")
    public ResponseEntity<List<EquipmentResponse>> getStudentAllowedEquipment(@AuthenticationPrincipal UserDetails userDetails) {
        User user = getCurrentUser(userDetails);
        List<EquipmentResponse> equipment = equipmentService.getStudentAllowedEquipmentByLocation(user.getLocation());
        return ResponseEntity.ok(equipment);
    }

    @GetMapping("/{id}")
    public ResponseEntity<EquipmentResponse> getEquipmentById(@PathVariable Long id) {
        EquipmentResponse equipment = equipmentService.getEquipmentById(id);
        return ResponseEntity.ok(equipment);
    }

    @PostMapping
    public ResponseEntity<EquipmentResponse> createEquipment(@Valid @RequestBody EquipmentRequest request, 
                                                           @AuthenticationPrincipal UserDetails userDetails) {
        User user = getCurrentUser(userDetails);
        EquipmentResponse equipment = equipmentService.createEquipment(request, user);
        return ResponseEntity.ok(equipment);
    }

    @PutMapping("/{id}")
    public ResponseEntity<EquipmentResponse> updateEquipment(@PathVariable Long id, 
                                                           @Valid @RequestBody EquipmentRequest request,
                                                           @AuthenticationPrincipal UserDetails userDetails) {
        User user = getCurrentUser(userDetails);
        EquipmentResponse equipment = equipmentService.updateEquipment(id, request, user);
        return ResponseEntity.ok(equipment);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<MessageResponse> deleteEquipment(@PathVariable Long id, 
                                                         @AuthenticationPrincipal UserDetails userDetails) {
        User user = getCurrentUser(userDetails);
        MessageResponse response = equipmentService.deleteEquipment(id, user);
        return ResponseEntity.ok(response);
    }

    //  Update equipment status (move quantities between statuses)
    @PutMapping("/{id}/status")
    public ResponseEntity<EquipmentResponse> updateEquipmentStatus(@PathVariable Long id,
                                                                 @Valid @RequestBody EquipmentStatusUpdateRequest request,
                                                                 @AuthenticationPrincipal UserDetails userDetails) {
        User user = getCurrentUser(userDetails);
        EquipmentResponse equipment = equipmentService.updateEquipmentStatus(id, request, user);
        return ResponseEntity.ok(equipment);
    }

    //  Get equipment history
    @GetMapping("/{id}/history")
    public ResponseEntity<List<EquipmentLog>> getEquipmentHistory(@PathVariable Long id,
                                                                @AuthenticationPrincipal UserDetails userDetails) {
        User user = getCurrentUser(userDetails);
        List<EquipmentLog> history = equipmentService.getEquipmentHistory(id, user);
        return ResponseEntity.ok(history);
    }

    @GetMapping("/search")
    public ResponseEntity<List<EquipmentResponse>> searchEquipment(@RequestParam String keyword,
                                                                 @AuthenticationPrincipal UserDetails userDetails) {
        User user = getCurrentUser(userDetails);
        List<EquipmentResponse> equipment = equipmentService.searchEquipmentByLocation(keyword, user.getLocation());
        return ResponseEntity.ok(equipment);
    }

    // Legacy endpoints for backward compatibility
    @PostMapping("/{id}/toggle-availability")
    public ResponseEntity<EquipmentResponse> toggleEquipmentAvailability(@PathVariable Long id) {
        EquipmentResponse equipment = equipmentService.toggleEquipmentAvailability(id);
        return ResponseEntity.ok(equipment);
    }


    //  Get available equipment units for request approval
    @GetMapping("/{equipmentId}/available-units")
    public ResponseEntity<List<EquipmentUnitResponse>> getAvailableUnitsForRequest(
            @PathVariable Long equipmentId,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = getCurrentUser(userDetails);
        List<EquipmentUnitResponse> units = equipmentUnitService.getAvailableUnitsForEquipment(equipmentId, user);
        return ResponseEntity.ok(units);
    }

    //  Enhanced approval with serial number selection
    @PostMapping("/requests/{requestId}/approve-with-serial")
    public ResponseEntity<MessageResponse> approveRequestWithSerial(
            @PathVariable Long requestId,
            @Valid @RequestBody EquipmentRequestApprovalRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = getCurrentUser(userDetails);
        
        // Use enhanced approval method
        MessageResponse response = equipmentService.handleEquipmentRequestApprovalWithSerial(
                requestId, request, request.getSelectedEquipmentUnitId(), user.getEmail());
        return ResponseEntity.ok(response);
    }

    //  Return equipment with serial number verification
    @PostMapping("/requests/{requestId}/return")
    public ResponseEntity<MessageResponse> returnEquipment(
            @PathVariable Long requestId,
            @RequestParam String returnCondition,
            @RequestParam(required = false) String returnNotes,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = getCurrentUser(userDetails);
        
        MessageResponse response = equipmentService.returnEquipmentWithSerial(
                requestId, returnCondition, returnNotes, user.getEmail());
        return ResponseEntity.ok(response);
    }

    //  Bulk approval with serial number mapping
    @PostMapping("/requests/bulk-approve")
    public ResponseEntity<MessageResponse> bulkApproveRequests(
            @RequestBody Map<String, Object> requestData,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = getCurrentUser(userDetails);
        
        @SuppressWarnings("unchecked")
        List<Long> requestIds = (List<Long>) requestData.get("requestIds");
        
        @SuppressWarnings("unchecked")
        Map<Long, Long> requestToUnitMapping = (Map<Long, Long>) requestData.get("unitMapping");
        
        MessageResponse response = equipmentService.bulkApproveRequestsWithSerials(
                requestIds, requestToUnitMapping, user.getEmail());
        return ResponseEntity.ok(response);
    }

    //  Check equipment availability for time period
    @GetMapping("/{equipmentId}/availability")
    public ResponseEntity<Map<String, Object>> checkEquipmentAvailability(
            @PathVariable Long equipmentId,
            @RequestParam int quantity,
            @RequestParam String startTime,
            @RequestParam String endTime,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        try {
            LocalDateTime start = LocalDateTime.parse(startTime);
            LocalDateTime end = LocalDateTime.parse(endTime);
            
            boolean available = equipmentService.isEquipmentAvailableForRequest(
                    equipmentId, quantity, start, end);
            
            Map<String, Object> response = new HashMap<>();
            response.put("available", available);
            response.put("equipmentId", equipmentId);
            response.put("requestedQuantity", quantity);
            response.put("timeSlot", Map.of("start", startTime, "end", endTime));
            
            if (available) {
                // Get available units
                List<EquipmentUnitResponse> availableUnits = equipmentUnitService
                        .getAvailableUnitsForEquipment(equipmentId, getCurrentUser(userDetails));
                response.put("availableUnits", availableUnits);
            }
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid time format"));
        }
    }
}