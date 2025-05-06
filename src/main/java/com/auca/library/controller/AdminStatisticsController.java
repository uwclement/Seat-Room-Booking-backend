package com.auca.library.controller;

// import com.auca.library.dto.response.StatisticsResponse;
// import com.auca.library.service.StatisticsService;
// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.format.annotation.DateTimeFormat;
// import org.springframework.http.ResponseEntity;
// import org.springframework.security.access.prepost.PreAuthorize;
// import org.springframework.web.bind.annotation.*;

// import java.time.LocalDate;
// import java.util.Map;

// @CrossOrigin(origins = "*", maxAge = 3600)
// @RestController
// @RequestMapping("/api/admin/statistics")
// @PreAuthorize("hasRole('ADMIN')")
public class AdminStatisticsController {

    // @Autowired
    // private StatisticsService statisticsService;

    // // Get daily statistics
    // @GetMapping("/daily")
    // public ResponseEntity<StatisticsResponse> getDailyStatistics(
    //         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
    //     if (date == null) {
    //         date = LocalDate.now();
    //     }
    //     return ResponseEntity.ok(statisticsService.getDailyStatistics(date));
    // }

    // // Get weekly statistics
    // @GetMapping("/weekly")
    // public ResponseEntity<Map<String, StatisticsResponse>> getWeeklyStatistics(
    //         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate) {
    //     if (startDate == null) {
    //         startDate = LocalDate.now().minusDays(LocalDate.now().getDayOfWeek().getValue() - 1);
    //     }
    //     return ResponseEntity.ok(statisticsService.getWeeklyStatistics(startDate));
    // }

    // // Get monthly statistics
    // @GetMapping("/monthly")
    // public ResponseEntity<Map<String, StatisticsResponse>> getMonthlyStatistics(
    //         @RequestParam(required = false) Integer year,
    //         @RequestParam(required = false) Integer month) {
    //     if (year == null) {
    //         year = LocalDate.now().getYear();
    //     }
    //     if (month == null) {
    //         month = LocalDate.now().getMonthValue();
    //     }
    //     return ResponseEntity.ok(statisticsService.getMonthlyStatistics(year, month));
    // }

    // // Get peak usage hours
    // @GetMapping("/peak-hours")
    // public ResponseEntity<Map<Integer, Integer>> getPeakHours(
    //         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
    //     if (date == null) {
    //         date = LocalDate.now();
    //     }
    //     return ResponseEntity.ok(statisticsService.getPeakHours(date));
    // }

    // // Get zone utilization statistics
    // @GetMapping("/zone-utilization")
    // public ResponseEntity<Map<String, Double>> getZoneUtilization(
    //         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
    //     if (date == null) {
    //         date = LocalDate.now();
    //     }
    //     return ResponseEntity.ok(statisticsService.getZoneUtilization(date));
    // }

    // // Get seat utilization
    // @GetMapping("/seat-utilization")
    // public ResponseEntity<Map<Long, Double>> getSeatUtilization(
    //         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
    //     if (date == null) {
    //         date = LocalDate.now();
    //     }
    //     return ResponseEntity.ok(statisticsService.getSeatUtilization(date));
    // }

    // // Get average booking duration
    // @GetMapping("/average-booking-duration")
    // public ResponseEntity<Map<String, Double>> getAverageBookingDuration(
    //         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
    //         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
    //     if (startDate == null) {
    //         startDate = LocalDate.now().minusDays(30);
    //     }
    //     if (endDate == null) {
    //         endDate = LocalDate.now();
    //     }
    //     return ResponseEntity.ok(statisticsService.getAverageBookingDuration(startDate, endDate));
    // }
}