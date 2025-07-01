package com.auca.library.controller;

import com.auca.library.dto.request.EquipmentRequestRequest;
import com.auca.library.dto.request.EquipmentRequestApprovalRequest;
import com.auca.library.dto.response.EquipmentRequestResponse;
import com.auca.library.dto.response.MessageResponse;
import com.auca.library.service.EquipmentRequestService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/equipment-requests")
public class EquipmentRequestController {

    @Autowired
    private EquipmentRequestService equipmentRequestService;

    // Create equipment request (Professor/Student)
    @PostMapping
    @PreAuthorize("hasRole('PROFESSOR') or hasRole('USER')")
    public ResponseEntity<EquipmentRequestResponse> createEquipmentRequest(
            @Valid @RequestBody EquipmentRequestRequest request,
            Authentication authentication) {
        EquipmentRequestResponse response = equipmentRequestService.createEquipmentRequest(request, authentication.getName());
        return ResponseEntity.ok(response);
    }

    // Get current user's requests
    @GetMapping("/my-requests")
    @PreAuthorize("hasRole('PROFESSOR') or hasRole('USER')")
    public ResponseEntity<List<EquipmentRequestResponse>> getCurrentUserRequests(Authentication authentication) {
        List<EquipmentRequestResponse> requests = equipmentRequestService.getCurrentUserRequests(authentication.getName());
        return ResponseEntity.ok(requests);
    }

    // Escalate to HOD (Professor only)
    @PostMapping("/{requestId}/escalate")
    @PreAuthorize("hasRole('PROFESSOR')")
    public ResponseEntity<MessageResponse> escalateToHod(@PathVariable Long requestId, Authentication authentication) {
        MessageResponse response = equipmentRequestService.escalateToHod(requestId, authentication.getName());
        return ResponseEntity.ok(response);
    }

    // Equipment Admin endpoints
    @GetMapping("/pending")
    @PreAuthorize("hasRole('EQUIPMENT_ADMIN')")
    public ResponseEntity<List<EquipmentRequestResponse>> getPendingRequests() {
        List<EquipmentRequestResponse> requests = equipmentRequestService.getPendingRequests();
        return ResponseEntity.ok(requests);
    }

    @PostMapping("/{requestId}/approve")
    @PreAuthorize("hasRole('EQUIPMENT_ADMIN')")
    public ResponseEntity<MessageResponse> handleEquipmentRequestApproval(
            @PathVariable Long requestId,
            @Valid @RequestBody EquipmentRequestApprovalRequest request,
            Authentication authentication) {
        MessageResponse response = equipmentRequestService.handleEquipmentRequestApproval(requestId, request, authentication.getName());
        return ResponseEntity.ok(response);
    }

    // HOD endpoints
    @GetMapping("/escalated")
    @PreAuthorize("hasRole('HOD')")
    public ResponseEntity<List<EquipmentRequestResponse>> getEscalatedRequests() {
        List<EquipmentRequestResponse> requests = equipmentRequestService.getEscalatedRequests();
        return ResponseEntity.ok(requests);
    }

    @PostMapping("/{requestId}/hod-review")
    @PreAuthorize("hasRole('HOD')")
    public ResponseEntity<MessageResponse> hodReviewEscalation(
            @PathVariable Long requestId,
            @Valid @RequestBody EquipmentRequestApprovalRequest request,
            Authentication authentication) {
        MessageResponse response = equipmentRequestService.hodReviewEscalation(requestId, request, authentication.getName());
        return ResponseEntity.ok(response);
    }
}