package com.auca.library.controller;

import com.auca.library.dto.response.*;
import com.auca.library.service.DashboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    @Autowired
    private DashboardService dashboardService;

    @GetMapping("/professor")
    @PreAuthorize("hasRole('PROFESSOR')")
    public ResponseEntity<ProfessorDashboardResponse> getProfessorDashboard(Authentication authentication) {
        ProfessorDashboardResponse dashboard = dashboardService.getProfessorDashboard(authentication.getName());
        return ResponseEntity.ok(dashboard);
    }

    @GetMapping("/equipment-admin")
    @PreAuthorize("hasRole('EQUIPMENT_ADMIN')")
    public ResponseEntity<EquipmentAdminDashboardResponse> getEquipmentAdminDashboard() {
        EquipmentAdminDashboardResponse dashboard = dashboardService.getEquipmentAdminDashboard();
        return ResponseEntity.ok(dashboard);
    }

    @GetMapping("/hod")
    @PreAuthorize("hasRole('HOD')")
    public ResponseEntity<HodDashboardResponse> getHodDashboard() {
        HodDashboardResponse dashboard = dashboardService.getHodDashboard();
        return ResponseEntity.ok(dashboard);
    }
}
