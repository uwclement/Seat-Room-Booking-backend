package com.auca.library.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
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

import com.auca.library.dto.request.EquipmentAssignmentRequest;
import com.auca.library.dto.request.EquipmentUnitRequest;
import com.auca.library.dto.response.EquipmentAssignmentResponse;
import com.auca.library.dto.response.EquipmentSummaryResponse;
import com.auca.library.dto.response.EquipmentUnitResponse;
import com.auca.library.dto.response.MessageResponse;
import com.auca.library.model.Location;
import com.auca.library.model.User;
import com.auca.library.service.EquipmentReportService;
import com.auca.library.service.EquipmentUnitService;
import com.auca.library.service.UserService;

import jakarta.validation.Valid;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/equipment-admin/equipment-units")
@PreAuthorize("hasRole('EQUIPMENT_ADMIN') or hasRole('ADMIN')")
public class EquipmentUnitController {

    @Autowired
    private EquipmentUnitService equipmentUnitService;
    
    @Autowired
    private EquipmentReportService reportService;
    
    @Autowired
    private UserService userService;

    // Helper method to get current user
    private User getCurrentUser(UserDetails userDetails) {
        return userService.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found: " + userDetails.getUsername()));
    }

    // Create new equipment unit
    @PostMapping
    public ResponseEntity<EquipmentUnitResponse> createEquipmentUnit(
            @Valid @RequestBody EquipmentUnitRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = getCurrentUser(userDetails);
        EquipmentUnitResponse response = equipmentUnitService.createEquipmentUnit(request, user);
        return ResponseEntity.ok(response);
    }

    // Get equipment units by location (automatic location filtering for equipment admin)
    @GetMapping
    public ResponseEntity<List<EquipmentUnitResponse>> getEquipmentUnits(
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = getCurrentUser(userDetails);
        List<EquipmentUnitResponse> units = equipmentUnitService.getEquipmentUnitsByLocation(user.getLocation(), user);
        return ResponseEntity.ok(units);
    }

    // Get available units for specific equipment (for request approval)
    @GetMapping("/available/{equipmentId}")
    public ResponseEntity<List<EquipmentUnitResponse>> getAvailableUnitsForEquipment(
            @PathVariable Long equipmentId,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = getCurrentUser(userDetails);
        List<EquipmentUnitResponse> units = equipmentUnitService.getAvailableUnitsForEquipment(equipmentId, user);
        return ResponseEntity.ok(units);
    }

    // Assign equipment unit
    @PostMapping("/assign")
    public ResponseEntity<EquipmentAssignmentResponse> assignEquipmentUnit(
            @Valid @RequestBody EquipmentAssignmentRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = getCurrentUser(userDetails);
        EquipmentAssignmentResponse response = equipmentUnitService.assignEquipmentUnit(request, user);
        return ResponseEntity.ok(response);
    }

    // Remove assignment (return equipment)
    @PutMapping("/assignments/{assignmentId}/remove")
    public ResponseEntity<MessageResponse> removeAssignment(
            @PathVariable Long assignmentId,
            @RequestParam String returnReason,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = getCurrentUser(userDetails);
        MessageResponse response = equipmentUnitService.removeAssignment(assignmentId, returnReason, user);
        return ResponseEntity.ok(response);
    }

    // Get active assignments
    @GetMapping("/assignments")
    public ResponseEntity<List<EquipmentAssignmentResponse>> getActiveAssignments(
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = getCurrentUser(userDetails);
        List<EquipmentAssignmentResponse> assignments = equipmentUnitService.getAssignmentsByLocation(user.getLocation(), user);
        return ResponseEntity.ok(assignments);
    }

    // Equipment summary dashboard
    @GetMapping("/summary")
    public ResponseEntity<EquipmentSummaryResponse> getEquipmentSummary(
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = getCurrentUser(userDetails);
        // This would need implementation in the service
        // For now, return basic response
        EquipmentSummaryResponse summary = new EquipmentSummaryResponse();
        summary.setLocation(user.getLocation());
        return ResponseEntity.ok(summary);
    }

    // Generate and download PDF reports
    @GetMapping("/reports/pdf")
    public ResponseEntity<byte[]> generateEquipmentReport(
            @RequestParam(defaultValue = "INVENTORY") String reportType,
            @RequestParam(defaultValue = "false") boolean detailed,
            @RequestParam(required = false) Location location,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        User user = getCurrentUser(userDetails);
        
        // For equipment admin, use their location. For admin, use specified location or their own
        Location targetLocation = location;
        if (targetLocation == null || !isAdmin(user)) {
            targetLocation = user.getLocation();
        }
        
        try {
            byte[] pdfBytes = reportService.generateEquipmentReportPDF(targetLocation, reportType, detailed, user);
            
            String filename = String.format("equipment_%s_report_%s_%s.pdf", 
                    reportType.toLowerCase(),
                    targetLocation.name().toLowerCase(),
                    detailed ? "detailed" : "summary");
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("inline", filename); // inline for preview
            headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(pdfBytes);
                    
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    // Admin-only: Get reports for any location
    @GetMapping("/reports/pdf/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<byte[]> generateAdminEquipmentReport(
            @RequestParam Location location,
            @RequestParam(defaultValue = "INVENTORY") String reportType,
            @RequestParam(defaultValue = "false") boolean detailed,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        User user = getCurrentUser(userDetails);
        
        try {
            byte[] pdfBytes = reportService.generateEquipmentReportPDF(location, reportType, detailed, user);
            
            String filename = String.format("equipment_%s_report_%s_%s.pdf", 
                    reportType.toLowerCase(),
                    location.name().toLowerCase(),
                    detailed ? "detailed" : "summary");
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("inline", filename);
            headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(pdfBytes);
                    
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    // Helper method to check if user is admin
    private boolean isAdmin(User user) {
        return user.getRoles().stream()
                .anyMatch(role -> role.getName().name().equals("ROLE_ADMIN"));
    }
}