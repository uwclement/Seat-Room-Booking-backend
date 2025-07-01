package com.auca.library.controller;

import com.auca.library.dto.response.EquipmentUsageStatsResponse;
import com.auca.library.service.AnalyticsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/analytics")
@PreAuthorize("hasRole('EQUIPMENT_ADMIN') or hasRole('HOD')")
public class AnalyticsController {

    @Autowired
    private AnalyticsService analyticsService;

    @GetMapping("/equipment-usage")
    public ResponseEntity<List<EquipmentUsageStatsResponse>> getEquipmentUsageStats(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        
        if (startDate == null) startDate = LocalDateTime.now().minusMonths(1);
        if (endDate == null) endDate = LocalDateTime.now();
        
        List<EquipmentUsageStatsResponse> stats = analyticsService.getEquipmentUsageStats(startDate, endDate);
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/system-overview")
    public ResponseEntity<Map<String, Object>> getSystemOverview() {
        Map<String, Object> stats = analyticsService.getOverallSystemStats();
        return ResponseEntity.ok(stats);
    }
}