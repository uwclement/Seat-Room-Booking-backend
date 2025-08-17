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
import com.auca.library.dto.request.EquipmentRequestRequest;
import com.auca.library.dto.response.EquipmentRequestResponse;
import com.auca.library.dto.response.MessageResponse;
import com.auca.library.service.EquipmentRequestService;

import jakarta.validation.Valid;

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

    @GetMapping("/current-month")
    @PreAuthorize("hasRole('EQUIPMENT_ADMIN')")
    public ResponseEntity<List<EquipmentRequestResponse>> getCurrentMonthRequests() {
        List<EquipmentRequestResponse> requests = equipmentRequestService.getCurrentMonthRequests();
      return ResponseEntity.ok(requests);
    }

     @GetMapping("/hod-current-month")
    @PreAuthorize("hasRole('HOD')")
    public ResponseEntity<List<EquipmentRequestResponse>> getHodCurrentMonthRequests() {
        List<EquipmentRequestResponse> requests = equipmentRequestService.getHodCurrentMonthRequests();
      return ResponseEntity.ok(requests);
    }


// Cancel request
@PostMapping("/{requestId}/cancel")
@PreAuthorize("hasRole('PROFESSOR') or hasRole('USER')")
public ResponseEntity<MessageResponse> cancelRequest(@PathVariable Long requestId, Authentication authentication) {
    MessageResponse response = equipmentRequestService.cancelRequest(requestId, authentication.getName());
    return ResponseEntity.ok(response);
}

// Get request by ID
@GetMapping("/{requestId}")
@PreAuthorize("hasRole('PROFESSOR') or hasRole('USER') or hasRole('EQUIPMENT_ADMIN')")
public ResponseEntity<EquipmentRequestResponse> getRequestById(@PathVariable Long requestId) {
    EquipmentRequestResponse response = equipmentRequestService.getRequestById(requestId);
    return ResponseEntity.ok(response);
}

// Complete request
@PostMapping("/{requestId}/complete")
@PreAuthorize("hasRole('EQUIPMENT_ADMIN')")
public ResponseEntity<MessageResponse> completeRequest(@PathVariable Long requestId, Authentication authentication) {
    equipmentRequestService.completeRequest(requestId);
    return ResponseEntity.ok(new MessageResponse("Request completed successfully"));
}


// Respond to admin suggestion (Professor)
@PostMapping("/{requestId}/respond-suggestion")
@PreAuthorize("hasRole('PROFESSOR')")
public ResponseEntity<MessageResponse> respondToSuggestion(
        @PathVariable Long requestId,
        @Valid @RequestBody EquipmentRequestRequest request,
        Authentication authentication) {
    MessageResponse response = equipmentRequestService.respondToSuggestion(
        requestId, request, authentication.getName());
    return ResponseEntity.ok(response);
}

// Request extension (Professor)
@PostMapping("/{requestId}/request-extension")
@PreAuthorize("hasRole('PROFESSOR')")
public ResponseEntity<MessageResponse> requestExtension(
        @PathVariable Long requestId,
        @Valid @RequestBody EquipmentRequestRequest request,
        Authentication authentication) {
    MessageResponse response = equipmentRequestService.requestExtension(
        requestId, request, authentication.getName());
    return ResponseEntity.ok(response);
}

// Mark equipment as returned (Equipment Admin)
@PostMapping("/{requestId}/mark-returned")
@PreAuthorize("hasRole('EQUIPMENT_ADMIN')")
public ResponseEntity<MessageResponse> markEquipmentReturned(
        @PathVariable Long requestId,
        @Valid @RequestBody EquipmentRequestRequest request,
        Authentication authentication) {
    MessageResponse response = equipmentRequestService.markEquipmentReturned(
        requestId, request, authentication.getName());
    return ResponseEntity.ok(response);
}

// Handle extension requests (Equipment Admin)
@PostMapping("/{requestId}/handle-extension")
@PreAuthorize("hasRole('EQUIPMENT_ADMIN')")
public ResponseEntity<MessageResponse> handleExtensionRequest(
        @PathVariable Long requestId,
        @Valid @RequestBody EquipmentRequestRequest request,
        Authentication authentication) {
    MessageResponse response = equipmentRequestService.handleExtensionRequest(
        requestId, request, authentication.getName());
    return ResponseEntity.ok(response);
}

// Get active/in-use requests (Equipment Admin)
@GetMapping("/active")
@PreAuthorize("hasRole('EQUIPMENT_ADMIN')")
public ResponseEntity<List<EquipmentRequestResponse>> getActiveRequests() {
    List<EquipmentRequestResponse> requests = equipmentRequestService.getActiveRequests();
    return ResponseEntity.ok(requests);
}

// Get extension requests (Equipment Admin)
@GetMapping("/extension-requests")
@PreAuthorize("hasRole('EQUIPMENT_ADMIN')")
public ResponseEntity<List<EquipmentRequestResponse>> getExtensionRequests() {
    List<EquipmentRequestResponse> requests = equipmentRequestService.getExtensionRequests();
    return ResponseEntity.ok(requests);
}

}