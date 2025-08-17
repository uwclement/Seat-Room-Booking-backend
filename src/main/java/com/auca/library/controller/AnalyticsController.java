package com.auca.library.controller;

import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.auca.library.dto.response.analytics.AnalyticsFilterRequest;
import com.auca.library.dto.response.analytics.EquipmentAnalyticsSummary;
import com.auca.library.dto.response.analytics.EquipmentChartsData;
import com.auca.library.dto.response.analytics.RoomAnalyticsSummary;
import com.auca.library.dto.response.analytics.RoomChartsData;
import com.auca.library.dto.response.analytics.SeatAnalyticsSummary;
import com.auca.library.dto.response.analytics.SeatChartsData;
import com.auca.library.dto.response.analytics.UserAnalyticsSummary;
import com.auca.library.dto.response.analytics.UserChartsData;
import com.auca.library.model.Location;
import com.auca.library.model.User;
import com.auca.library.service.UserService;
import com.auca.library.service.analytics.EquipmentAnalyticsService;
import com.auca.library.service.analytics.RoomAnalyticsService;
import com.auca.library.service.analytics.SeatAnalyticsService;
import com.auca.library.service.analytics.UserAnalyticsService;

@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {

    @Autowired
    private SeatAnalyticsService seatAnalyticsService;
    
    @Autowired
    private RoomAnalyticsService roomAnalyticsService;
    
    @Autowired
    private EquipmentAnalyticsService equipmentAnalyticsService;
    
    @Autowired
    private UserAnalyticsService userAnalyticsService;
    
    @Autowired
    private UserService userService;

    // ===== SEAT ANALYTICS ENDPOINTS =====

    @GetMapping("/seats/summary")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_LIBRARIAN')")
    public ResponseEntity<SeatAnalyticsSummary> getSeatSummary(
            @RequestParam(required = false) String location,
            @RequestParam(required = false) String period,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            Authentication authentication) {
        
        AnalyticsFilterRequest filter = createFilterRequest(location, period, startDate, endDate, authentication);
        SeatAnalyticsSummary summary = seatAnalyticsService.getSummary(filter);
        return ResponseEntity.ok(summary);
    }

    @GetMapping("/seats/charts")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_LIBRARIAN')")
    public ResponseEntity<SeatChartsData> getSeatCharts(
            @RequestParam(required = false) String location,
            @RequestParam(required = false) String period,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            Authentication authentication) {
        
        AnalyticsFilterRequest filter = createFilterRequest(location, period, startDate, endDate, authentication);
        SeatChartsData charts = seatAnalyticsService.getChartsData(filter);
        return ResponseEntity.ok(charts);
    }

    @PostMapping("/seats/report/simple")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_LIBRARIAN')")
    public ResponseEntity<byte[]> downloadSimpleSeatReport(
            @RequestBody AnalyticsFilterRequest filter,
            Authentication authentication) {
        
        filter = applyLocationRestriction(filter, authentication);
        byte[] pdfBytes = seatAnalyticsService.generateSimpleReport(filter);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "seat-analytics-simple.pdf");
        
        return ResponseEntity.ok()
                .headers(headers)
                .body(pdfBytes);
    }

    @PostMapping("/seats/report/detailed")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_LIBRARIAN')")
    public ResponseEntity<byte[]> downloadDetailedSeatReport(
            @RequestBody AnalyticsFilterRequest filter,
            Authentication authentication) {
        
        filter = applyLocationRestriction(filter, authentication);
        byte[] pdfBytes = seatAnalyticsService.generateDetailedReport(filter);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "seat-analytics-detailed.pdf");
        
        return ResponseEntity.ok()
                .headers(headers)
                .body(pdfBytes);
    }

    // ===== ROOM ANALYTICS ENDPOINTS =====

    @GetMapping("/rooms/summary")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_LIBRARIAN')")
    public ResponseEntity<RoomAnalyticsSummary> getRoomSummary(
            @RequestParam(required = false) String location,
            @RequestParam(required = false) String period,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            Authentication authentication) {
        
        AnalyticsFilterRequest filter = createFilterRequest(location, period, startDate, endDate, authentication);
        RoomAnalyticsSummary summary = roomAnalyticsService.getSummary(filter);
        return ResponseEntity.ok(summary);
    }

    @GetMapping("/rooms/charts")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_LIBRARIAN')")
    public ResponseEntity<RoomChartsData> getRoomCharts(
            @RequestParam(required = false) String location,
            @RequestParam(required = false) String period,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            Authentication authentication) {
        
        AnalyticsFilterRequest filter = createFilterRequest(location, period, startDate, endDate, authentication);
        RoomChartsData charts = roomAnalyticsService.getChartsData(filter);
        return ResponseEntity.ok(charts);
    }

    @PostMapping("/rooms/report/simple")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_LIBRARIAN')")
    public ResponseEntity<byte[]> downloadSimpleRoomReport(
            @RequestBody AnalyticsFilterRequest filter,
            Authentication authentication) {
        
        filter = applyLocationRestriction(filter, authentication);
        byte[] pdfBytes = roomAnalyticsService.generateSimpleReport(filter);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "room-analytics-simple.pdf");
        
        return ResponseEntity.ok()
                .headers(headers)
                .body(pdfBytes);
    }

    @PostMapping("/rooms/report/detailed")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_LIBRARIAN')")
    public ResponseEntity<byte[]> downloadDetailedRoomReport(
            @RequestBody AnalyticsFilterRequest filter,
            Authentication authentication) {
        
        filter = applyLocationRestriction(filter, authentication);
        byte[] pdfBytes = roomAnalyticsService.generateDetailedReport(filter);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "room-analytics-detailed.pdf");
        
        return ResponseEntity.ok()
                .headers(headers)
                .body(pdfBytes);
    }

    // ===== EQUIPMENT ANALYTICS ENDPOINTS =====

    @GetMapping("/equipment/summary")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EQUIPMENT_ADMIN')")
    public ResponseEntity<EquipmentAnalyticsSummary> getEquipmentSummary(
            @RequestParam(required = false) String location,
            @RequestParam(required = false) String period,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            Authentication authentication) {
        
        AnalyticsFilterRequest filter = createFilterRequest(location, period, startDate, endDate, authentication);
        EquipmentAnalyticsSummary summary = equipmentAnalyticsService.getSummary(filter);
        return ResponseEntity.ok(summary);
    }

    @GetMapping("/equipment/charts")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EQUIPMENT_ADMIN')")
    public ResponseEntity<EquipmentChartsData> getEquipmentCharts(
            @RequestParam(required = false) String location,
            @RequestParam(required = false) String period,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            Authentication authentication) {
        
        AnalyticsFilterRequest filter = createFilterRequest(location, period, startDate, endDate, authentication);
        EquipmentChartsData charts = equipmentAnalyticsService.getChartsData(filter);
        return ResponseEntity.ok(charts);
    }

    @PostMapping("/equipment/report/simple")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EQUIPMENT_ADMIN')")
    public ResponseEntity<byte[]> downloadSimpleEquipmentReport(
            @RequestBody AnalyticsFilterRequest filter,
            Authentication authentication) {
        
        filter = applyLocationRestriction(filter, authentication);
        byte[] pdfBytes = equipmentAnalyticsService.generateSimpleReport(filter);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "equipment-analytics-simple.pdf");
        
        return ResponseEntity.ok()
                .headers(headers)
                .body(pdfBytes);
    }

    @PostMapping("/equipment/report/detailed")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EQUIPMENT_ADMIN')")
    public ResponseEntity<byte[]> downloadDetailedEquipmentReport(
            @RequestBody AnalyticsFilterRequest filter,
            Authentication authentication) {
        
        filter = applyLocationRestriction(filter, authentication);
        byte[] pdfBytes = equipmentAnalyticsService.generateDetailedReport(filter);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "equipment-analytics-detailed.pdf");
        
        return ResponseEntity.ok()
                .headers(headers)
                .body(pdfBytes);
    }

    // ===== USER ANALYTICS ENDPOINTS =====

    @GetMapping("/users/summary")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_LIBRARIAN', 'ROLE_EQUIPMENT_ADMIN')")
    public ResponseEntity<UserAnalyticsSummary> getUserSummary(
            @RequestParam(required = false) String location,
            @RequestParam(required = false) String period,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            Authentication authentication) {
        
        AnalyticsFilterRequest filter = createFilterRequest(location, period, startDate, endDate, authentication);
        UserAnalyticsSummary summary = userAnalyticsService.getSummary(filter);
        return ResponseEntity.ok(summary);
    }

    @GetMapping("/users/charts")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_LIBRARIAN', 'ROLE_EQUIPMENT_ADMIN')")
    public ResponseEntity<UserChartsData> getUserCharts(
            @RequestParam(required = false) String location,
            @RequestParam(required = false) String period,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            Authentication authentication) {
        
        AnalyticsFilterRequest filter = createFilterRequest(location, period, startDate, endDate, authentication);
        UserChartsData charts = userAnalyticsService.getChartsData(filter);
        return ResponseEntity.ok(charts);
    }

    @PostMapping("/users/report/simple")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_LIBRARIAN', 'ROLE_EQUIPMENT_ADMIN')")
    public ResponseEntity<byte[]> downloadSimpleUserReport(
            @RequestBody AnalyticsFilterRequest filter,
            Authentication authentication) {
        
        filter = applyLocationRestriction(filter, authentication);
        byte[] pdfBytes = userAnalyticsService.generateSimpleReport(filter);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "user-analytics-simple.pdf");
        
        return ResponseEntity.ok()
                .headers(headers)
                .body(pdfBytes);
    }

    @PostMapping("/users/report/detailed")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_LIBRARIAN', 'ROLE_EQUIPMENT_ADMIN')")
    public ResponseEntity<byte[]> downloadDetailedUserReport(
            @RequestBody AnalyticsFilterRequest filter,
            Authentication authentication) {
        
        filter = applyLocationRestriction(filter, authentication);
        byte[] pdfBytes = userAnalyticsService.generateDetailedReport(filter);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "user-analytics-detailed.pdf");
        
        return ResponseEntity.ok()
                .headers(headers)
                .body(pdfBytes);
    }

    // ===== UTILITY ENDPOINTS =====

    @GetMapping("/user-permissions")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_LIBRARIAN', 'ROLE_EQUIPMENT_ADMIN')")
    public ResponseEntity<UserPermissionsResponse> getUserPermissions(Authentication authentication) {
        User currentUser = userService.getCurrentUser(authentication);
        
        UserPermissionsResponse permissions = new UserPermissionsResponse();
        permissions.setCanViewAllLocations(isAdmin(currentUser));
        permissions.setUserLocation(currentUser.getLocation().toString());
        permissions.setCanAccessSeats(isAdmin(currentUser) || isLibrarian(currentUser));
        permissions.setCanAccessRooms(isAdmin(currentUser) || isLibrarian(currentUser));
        permissions.setCanAccessEquipment(isAdmin(currentUser) || isEquipmentAdmin(currentUser));
        permissions.setCanAccessUsers(isAdmin(currentUser) || isLibrarian(currentUser) || isEquipmentAdmin(currentUser));
        
        return ResponseEntity.ok(permissions);
    }

    // ===== HELPER METHODS =====

    private AnalyticsFilterRequest createFilterRequest(String location, String period, 
            String startDate, String endDate, Authentication authentication) {
        
        AnalyticsFilterRequest filter = new AnalyticsFilterRequest();
        
        User currentUser = userService.getCurrentUser(authentication);
        
        // Apply location restriction for non-admin users
        if (isAdmin(currentUser)) {
            filter.setLocation(location != null ? location : "ALL");
        } else {
            // Non-admin users can only view their own location
            filter.setLocation(currentUser.getLocation().toString());
        }
        
        filter.setPeriod(period != null ? period : "WEEK");
        
        // Parse dates if provided
        if (startDate != null && endDate != null) {
            try {
                filter.setStartDate(LocalDateTime.parse(startDate));
                filter.setEndDate(LocalDateTime.parse(endDate));
            } catch (Exception e) {
                // Use period-based dates if parsing fails
                filter.setStartDate(null);
                filter.setEndDate(null);
            }
        }
        
        return filter;
    }

    private AnalyticsFilterRequest applyLocationRestriction(AnalyticsFilterRequest filter, Authentication authentication) {
        User currentUser = userService.getCurrentUser(authentication);
        
        // Override location for non-admin users
        if (!isAdmin(currentUser)) {
            filter.setLocation(currentUser.getLocation().toString());
        }
        
        return filter;
    }

    private boolean isAdmin(User user) {
        return user.getRoles().stream()
                .anyMatch(role -> role.getName().name().equals("ROLE_ADMIN"));
    }

    private boolean isLibrarian(User user) {
        return user.getRoles().stream()
                .anyMatch(role -> role.getName().name().equals("ROLE_LIBRARIAN"));
    }

    private boolean isEquipmentAdmin(User user) {
        return user.getRoles().stream()
                .anyMatch(role -> role.getName().name().equals("ROLE_EQUIPMENT_ADMIN"));
    }

    // ===== RESPONSE DTO FOR USER PERMISSIONS =====

    public static class UserPermissionsResponse {
        private boolean canViewAllLocations;
        private String userLocation;
        private boolean canAccessSeats;
        private boolean canAccessRooms;
        private boolean canAccessEquipment;
        private boolean canAccessUsers;
        
        // Getters and Setters
        public boolean isCanViewAllLocations() { return canViewAllLocations; }
        public void setCanViewAllLocations(boolean canViewAllLocations) { this.canViewAllLocations = canViewAllLocations; }
        
        public String getUserLocation() { return userLocation; }
        public void setUserLocation(String userLocation) { this.userLocation = userLocation; }
        
        public boolean isCanAccessSeats() { return canAccessSeats; }
        public void setCanAccessSeats(boolean canAccessSeats) { this.canAccessSeats = canAccessSeats; }
        
        public boolean isCanAccessRooms() { return canAccessRooms; }
        public void setCanAccessRooms(boolean canAccessRooms) { this.canAccessRooms = canAccessRooms; }
        
        public boolean isCanAccessEquipment() { return canAccessEquipment; }
        public void setCanAccessEquipment(boolean canAccessEquipment) { this.canAccessEquipment = canAccessEquipment; }
        
        public boolean isCanAccessUsers() { return canAccessUsers; }
        public void setCanAccessUsers(boolean canAccessUsers) { this.canAccessUsers = canAccessUsers; }
    }
}