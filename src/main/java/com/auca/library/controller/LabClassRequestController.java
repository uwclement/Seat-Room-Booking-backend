package com.auca.library.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.auca.library.dto.request.EquipmentRequestApprovalRequest;
import com.auca.library.dto.request.LabRequestRequest;
import com.auca.library.dto.response.LabRequestResponse;
import com.auca.library.dto.response.MessageResponse;
import com.auca.library.service.LabClassRequestService;

import jakarta.validation.Valid;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/lab-requests")
public class LabClassRequestController {

    @Autowired
    private LabClassRequestService labClassRequestService;

    @PostMapping
    @PreAuthorize("hasRole('ROLE_PROFESSOR')")
    public ResponseEntity<LabRequestResponse> createLabRequest(
            @Valid @RequestBody LabRequestRequest request,
            Authentication authentication) {
        LabRequestResponse response = labClassRequestService.createLabClassRequest(request, authentication.getName());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/my-requests")
    @PreAuthorize("hasRole('ROLE_PROFESSOR')")
    public ResponseEntity<List<LabRequestResponse>> getCurrentUserRequests(Authentication authentication) {
        List<LabRequestResponse> requests = labClassRequestService.getCurrentUserRequests(authentication.getName());
        return ResponseEntity.ok(requests);
    }

    @GetMapping("/pending")
    @PreAuthorize("hasRole('ROLE_EQUIPMENT_ADMIN')")
    public ResponseEntity<List<LabRequestResponse>> getPendingRequests() {
        List<LabRequestResponse> requests = labClassRequestService.getPendingRequests();
        return ResponseEntity.ok(requests);
    }
    @GetMapping("/current-month")
    @PreAuthorize("hasRole('EQUIPMENT_ADMIN')")
    public ResponseEntity<List<LabRequestResponse>> getCurrentMonthRequests() {
        List<LabRequestResponse> requests = labClassRequestService.getCurrentMonthRequests();
      return ResponseEntity.ok(requests);
    }

    @PostMapping("/{requestId}/approve")
    @PreAuthorize("hasRole('ROLE_EQUIPMENT_ADMIN')")
    public ResponseEntity<MessageResponse> handleLabRequestApproval(
            @PathVariable Long requestId,
            @Valid @RequestBody EquipmentRequestApprovalRequest request,
            Authentication authentication) {
        MessageResponse response = labClassRequestService.handleLabRequestApproval(requestId, request, authentication.getName());
        return ResponseEntity.ok(response);
    }

    // Cancel request
    @PostMapping("/{requestId}/cancel")
    @PreAuthorize("hasRole('PROFESSOR') or hasRole('USER')")
    public ResponseEntity<MessageResponse> cancelRequest(@PathVariable Long requestId, Authentication authentication) {
        MessageResponse response = labClassRequestService.cancelRequest(requestId, authentication.getName());
        return ResponseEntity.ok(response);
    }
}