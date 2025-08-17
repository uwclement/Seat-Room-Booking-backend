package com.auca.library.service.analytics;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.auca.library.dto.response.analytics.AnalyticsCard;
import com.auca.library.dto.response.analytics.AnalyticsFilterRequest;
import com.auca.library.dto.response.analytics.ChartData;
import com.auca.library.dto.response.analytics.EquipmentAnalyticsSummary;
import com.auca.library.dto.response.analytics.EquipmentChartsData;
import com.auca.library.model.Equipment;
import com.auca.library.model.EquipmentAssignment;
import com.auca.library.model.EquipmentRequest;
import com.auca.library.model.EquipmentUnit;
import com.auca.library.model.Location;
import com.auca.library.repository.EquipmentAssignmentRepository;
import com.auca.library.repository.EquipmentRepository;
import com.auca.library.repository.EquipmentRequestRepository;
import com.auca.library.repository.EquipmentUnitRepository;

@Service
public class EquipmentAnalyticsService {

    @Autowired
    private EquipmentRepository equipmentRepository;
    
    @Autowired
    private EquipmentUnitRepository equipmentUnitRepository;
    
    @Autowired
    private EquipmentRequestRepository equipmentRequestRepository;
    
    @Autowired
    private EquipmentAssignmentRepository equipmentAssignmentRepository;
    
    @Autowired
    private AnalyticsReportService reportService;

    public EquipmentAnalyticsSummary getSummary(AnalyticsFilterRequest filter) {
        Location location = parseLocation(filter.getLocation());
        LocalDateTime[] dateRange = parseDateRange(filter);
        
        EquipmentAnalyticsSummary summary = new EquipmentAnalyticsSummary(
            location != null ? location.name() : "ALL", 
            formatDateRange(dateRange[0], dateRange[1])
        );
        
        List<AnalyticsCard> cards = new ArrayList<>();
        
        // Card 1: Equipment Types
        int equipmentTypes = getTotalEquipmentTypes(location);
        cards.add(createAnalyticsCard("Equipment Types", String.valueOf(equipmentTypes), 
            "stable", "📦", "blue"));
        
        // Card 2: Total Units
        int totalUnits = getTotalEquipmentUnits(location);
        int previousTotalUnits = getPreviousTotalUnits(location, dateRange);
        cards.add(createAnalyticsCard("Total Units", String.valueOf(totalUnits),
            calculateTrend(totalUnits, previousTotalUnits), "🔧", "green"));
        
        // Card 3: Available Units
        int availableUnits = getAvailableUnits(location);
        int previousAvailableUnits = getPreviousAvailableUnits(location, dateRange);
        cards.add(createAnalyticsCard("Available Units", String.valueOf(availableUnits),
            calculateTrend(availableUnits, previousAvailableUnits), "✅", "orange"));
        
        // Card 4: Active Requests
        int activeRequests = getActiveRequests(location, dateRange);
        int previousActiveRequests = getPreviousActiveRequests(location, dateRange);
        cards.add(createAnalyticsCard("Active Requests", String.valueOf(activeRequests),
            calculateTrend(activeRequests, previousActiveRequests), "📋", "purple"));
        
        summary.setSummaryCards(cards);
        return summary;
    }

    public EquipmentChartsData getChartsData(AnalyticsFilterRequest filter) {
        Location location = parseLocation(filter.getLocation());
        LocalDateTime[] dateRange = parseDateRange(filter);
        
        EquipmentChartsData chartsData = new EquipmentChartsData();
        
        // Chart 1: Equipment Unit Status Distribution
        chartsData.setUnitStatusChart(getUnitStatusChart(location));
        
        // Chart 2: Most Requested Equipment
        chartsData.setMostRequestedChart(getMostRequestedChart(location, dateRange));
        
        // Chart 3: Assignment Duration Trends
        chartsData.setAssignmentTrendsChart(getAssignmentTrendsChart(location, dateRange));
        
        return chartsData;
    }

    public byte[] generateSimpleReport(AnalyticsFilterRequest filter) {
        EquipmentAnalyticsSummary summary = getSummary(filter);
        EquipmentChartsData charts = getChartsData(filter);
        return reportService.generateEquipmentSimpleReport(summary, charts);
    }

    public byte[] generateDetailedReport(AnalyticsFilterRequest filter) {
        EquipmentAnalyticsSummary summary = getSummary(filter);
        EquipmentChartsData charts = getChartsData(filter);
        
        // Additional detailed data
        Location location = parseLocation(filter.getLocation());
        LocalDateTime[] dateRange = parseDateRange(filter);
        
        List<Map<String, Object>> equipmentTypeBreakdown = getEquipmentTypeBreakdown(location);
        List<Map<String, Object>> unitStatusDetails = getUnitStatusDetails(location);
        List<Map<String, Object>> maintenanceUnits = getMaintenanceRequiredUnits(location);
        List<Map<String, Object>> assignmentHistory = getRecentAssignmentHistory(location, dateRange, 20);
        List<Map<String, Object>> requestAnalysis = getRequestAnalysis(location, dateRange);
        
        return reportService.generateEquipmentDetailedReport(summary, charts, 
            equipmentTypeBreakdown, unitStatusDetails, maintenanceUnits, 
            assignmentHistory, requestAnalysis);
    }

    // ===== PRIVATE HELPER METHODS =====

    private int getTotalEquipmentTypes(Location location) {
        if (location != null) {
            return equipmentRepository.countByLocation(location);
        }
        return (int) equipmentRepository.count();
    }

    private int getTotalEquipmentUnits(Location location) {
        if (location != null) {
            return equipmentUnitRepository.countByEquipmentLocation(location);
        }
        return (int) equipmentUnitRepository.count();
    }

    private int getAvailableUnits(Location location) {
        if (location != null) {
            return equipmentUnitRepository.countByEquipmentLocationAndStatus(
                location, EquipmentUnit.UnitStatus.AVAILABLE);
        }
        return equipmentUnitRepository.countByStatus(EquipmentUnit.UnitStatus.AVAILABLE);
    }

    private int getActiveRequests(Location location, LocalDateTime[] dateRange) {
        if (location != null) {
            return equipmentRequestRepository.countActiveByLocationAndDateRange(
                location, dateRange[0], dateRange[1]);
        }
        return equipmentRequestRepository.countActiveByDateRange(dateRange[0], dateRange[1]);
    }

    private ChartData getUnitStatusChart(Location location) {
        Map<String, Long> statusDistribution;
        
        if (location != null) {
            statusDistribution = equipmentUnitRepository.findByEquipmentLocation(location).stream()
                .collect(Collectors.groupingBy(
                    unit -> unit.getStatus().toString(),
                    Collectors.counting()));
        } else {
            statusDistribution = equipmentUnitRepository.findAll().stream()
                .collect(Collectors.groupingBy(
                    unit -> unit.getStatus().toString(),
                    Collectors.counting()));
        }
        
        List<String> labels = new ArrayList<>(statusDistribution.keySet());
        List<Number> data = new ArrayList<>(statusDistribution.values());
        
        return new ChartData("pie", "Equipment Unit Status Distribution", labels, data);
    }

    private ChartData getMostRequestedChart(Location location, LocalDateTime[] dateRange) {
        List<EquipmentRequest> requests = getRequestsInRange(location, dateRange);
        
        Map<String, Long> equipmentRequests = requests.stream()
            .collect(Collectors.groupingBy(
                request -> request.getEquipment().getName(),
                Collectors.counting()));
        
        List<Map.Entry<String, Long>> sortedEntries = equipmentRequests.entrySet().stream()
            .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
            .limit(10)
            .collect(Collectors.toList());
        
        List<String> labels = sortedEntries.stream()
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());
        
        List<Number> data = sortedEntries.stream()
            .map(Map.Entry::getValue)
            .collect(Collectors.toList());
        
        return new ChartData("bar", "Most Requested Equipment", labels, data);
    }

    private ChartData getAssignmentTrendsChart(Location location, LocalDateTime[] dateRange) {
        List<EquipmentAssignment> assignments = getAssignmentsInRange(location, dateRange);
        
        Map<String, Long> monthlyAssignments = assignments.stream()
            .collect(Collectors.groupingBy(
                assignment -> assignment.getStartDate().format(DateTimeFormatter.ofPattern("MMM yyyy")),
                LinkedHashMap::new,
                Collectors.counting()));
        
        List<String> labels = new ArrayList<>(monthlyAssignments.keySet());
        List<Number> data = new ArrayList<>(monthlyAssignments.values());
        
        return new ChartData("line", "Assignment Trends", labels, data);
    }

    private List<Map<String, Object>> getUnitStatusDetails(Location location) {
        List<EquipmentUnit> units = location != null ? 
            equipmentUnitRepository.findByEquipmentLocation(location) :
            equipmentUnitRepository.findAll();
        
        return units.stream()
            .map(unit -> {
                Map<String, Object> data = new LinkedHashMap<>();
                data.put("serialNumber", unit.getSerialNumber());
                data.put("equipmentName", unit.getEquipment().getName());
                data.put("status", unit.getStatus().toString());
                data.put("condition", unit.getCondition());
                data.put("location", unit.getLocation().toString());
                
                // Check if currently assigned
                equipmentAssignmentRepository.findActiveByEquipmentUnit(unit)
                    .ifPresent(assignment -> {
                        data.put("assignedTo", assignment.getAssignedToName());
                        data.put("assignmentType", assignment.getAssignmentType().toString());
                        data.put("assignedDate", assignment.getStartDate());
                    });
                
                return data;
            })
            .collect(Collectors.toList());
    }

    private List<Map<String, Object>> getMaintenanceRequiredUnits(Location location) {
        List<EquipmentUnit> maintenanceUnits = location != null ? 
            equipmentUnitRepository.findByEquipmentLocationAndStatus(location, EquipmentUnit.UnitStatus.MAINTENANCE) :
            equipmentUnitRepository.findByStatus(EquipmentUnit.UnitStatus.MAINTENANCE);
        
        List<EquipmentUnit> damagedUnits = location != null ? 
            equipmentUnitRepository.findByEquipmentLocationAndStatus(location, EquipmentUnit.UnitStatus.DAMAGED) :
            equipmentUnitRepository.findByStatus(EquipmentUnit.UnitStatus.DAMAGED);
        
        List<EquipmentUnit> allMaintenanceUnits = new ArrayList<>();
        allMaintenanceUnits.addAll(maintenanceUnits);
        allMaintenanceUnits.addAll(damagedUnits);
        
        return allMaintenanceUnits.stream()
            .map(unit -> {
                Map<String, Object> data = new LinkedHashMap<>();
                data.put("serialNumber", unit.getSerialNumber());
                data.put("equipmentName", unit.getEquipment().getName());
                data.put("status", unit.getStatus().toString());
                data.put("condition", unit.getCondition());
                data.put("notes", unit.getNotes());
                data.put("purchaseDate", unit.getPurchaseDate());
                data.put("warrantyExpiry", unit.getWarrantyExpiry());
                return data;
            })
            .collect(Collectors.toList());
    }

    private List<Map<String, Object>> getRecentAssignmentHistory(Location location, 
            LocalDateTime[] dateRange, int limit) {
        List<EquipmentAssignment> assignments = getAssignmentsInRange(location, dateRange);
        
        return assignments.stream()
            .sorted((a1, a2) -> a2.getStartDate().compareTo(a1.getStartDate()))
            .limit(limit)
            .map(assignment -> {
                Map<String, Object> data = new LinkedHashMap<>();
                data.put("serialNumber", assignment.getEquipmentUnit().getSerialNumber());
                data.put("equipmentName", assignment.getEquipmentUnit().getEquipment().getName());
                data.put("assignedTo", assignment.getAssignedToName());
                data.put("assignmentType", assignment.getAssignmentType().toString());
                data.put("startDate", assignment.getStartDate());
                data.put("endDate", assignment.getEndDate());
                data.put("status", assignment.getStatus().toString());
                data.put("assignedBy", assignment.getAssignedBy().getFullName());
                
                if (assignment.getReturnedAt() != null) {
                    data.put("returnedAt", assignment.getReturnedAt());
                    data.put("returnReason", assignment.getReturnReason());
                }
                
                return data;
            })
            .collect(Collectors.toList());
    }

    private List<Map<String, Object>> getRequestAnalysis(Location location, LocalDateTime[] dateRange) {
        List<EquipmentRequest> requests = getRequestsInRange(location, dateRange);
        
        Map<String, Object> analysisData = new LinkedHashMap<>();
        analysisData.put("totalRequests", requests.size());
        
        Map<String, Long> statusBreakdown = requests.stream()
            .collect(Collectors.groupingBy(
                request -> request.getStatus().toString(),
                Collectors.counting()));
        
        analysisData.put("statusBreakdown", statusBreakdown);
        
        // Top requesters
        Map<String, Long> topRequesters = requests.stream()
            .collect(Collectors.groupingBy(
                request -> request.getUser().getFullName(),
                Collectors.counting()));
        
        List<Map<String, Object>> topRequestersList = topRequesters.entrySet().stream()
            .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
            .limit(10)
            .map(entry -> {
                Map<String, Object> data = new LinkedHashMap<>();
                data.put("userName", entry.getKey());
                data.put("requestCount", entry.getValue());
                return data;
            })
            .collect(Collectors.toList());
        
        analysisData.put("topRequesters", topRequestersList);
        
        // Average request duration
        double avgDuration = requests.stream()
            .filter(request -> request.getStartTime() != null && request.getEndTime() != null)
            .mapToDouble(request -> ChronoUnit.HOURS.between(
                request.getStartTime(), request.getEndTime()))
            .average()
            .orElse(0.0);
        
        analysisData.put("averageDurationHours", avgDuration);
        
        return List.of(analysisData);
    }

    private List<EquipmentRequest> getRequestsInRange(Location location, LocalDateTime[] dateRange) {
        if (location != null) {
            return equipmentRequestRepository.findByEquipmentLocationAndDateRange(
                location, dateRange[0], dateRange[1]);
        }
        return equipmentRequestRepository.findByStartTimeBetween(dateRange[0], dateRange[1]);
    }

    private List<EquipmentAssignment> getAssignmentsInRange(Location location, LocalDateTime[] dateRange) {
        if (location != null) {
            return equipmentAssignmentRepository.findByLocationAndDateRange(
                location, dateRange[0], dateRange[1]);
        }
        return equipmentAssignmentRepository.findByStartDateBetween(dateRange[0], dateRange[1]);
    }

    // ===== PREVIOUS PERIOD COMPARISON METHODS =====

    private int getPreviousTotalUnits(Location location, LocalDateTime[] dateRange) {
        // Equipment units don't change frequently, so return current count
        return getTotalEquipmentUnits(location);
    }

    private int getPreviousAvailableUnits(Location location, LocalDateTime[] dateRange) {
        // Calculate based on assignments in previous period
        long periodDays = ChronoUnit.DAYS.between(dateRange[0], dateRange[1]);
        LocalDateTime[] prevRange = {
            dateRange[0].minusDays(periodDays),
            dateRange[0]
        };
        
        // Simplified calculation - could be more sophisticated
        List<EquipmentAssignment> prevAssignments = getAssignmentsInRange(location, prevRange);
        int totalUnits = getTotalEquipmentUnits(location);
        return Math.max(0, totalUnits - prevAssignments.size());
    }

    private int getPreviousActiveRequests(Location location, LocalDateTime[] dateRange) {
        long periodDays = ChronoUnit.DAYS.between(dateRange[0], dateRange[1]);
        LocalDateTime[] prevRange = {
            dateRange[0].minusDays(periodDays),
            dateRange[0]
        };
        return getActiveRequests(location, prevRange);
    }

    // ===== UTILITY METHODS =====

    private Location parseLocation(String locationStr) {
        if (locationStr == null || "ALL".equals(locationStr)) {
            return null;
        }
        try {
            return Location.valueOf(locationStr);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private LocalDateTime[] parseDateRange(AnalyticsFilterRequest filter) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime start, end;
        
        if (filter.getStartDate() != null && filter.getEndDate() != null) {
            start = filter.getStartDate();
            end = filter.getEndDate();
        } else {
            switch (filter.getPeriod() != null ? filter.getPeriod() : "WEEK") {
                case "TODAY":
                    start = now.toLocalDate().atStartOfDay();
                    end = now;
                    break;
                case "MONTH":
                    start = now.minusMonths(1);
                    end = now;
                    break;
                case "QUARTER":
                    start = now.minusMonths(3);
                    end = now;
                    break;
                case "YEAR":
                    start = now.minusYears(1);
                    end = now;
                    break;
                default: // WEEK
                    start = now.minusWeeks(1);
                    end = now;
                    break;
            }
        }
        
        return new LocalDateTime[]{start, end};
    }

    private String formatDateRange(LocalDateTime start, LocalDateTime end) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy");
        return start.format(formatter) + " - " + end.format(formatter);
    }

    private AnalyticsCard createAnalyticsCard(String title, String value, String trend, String icon, String color) {
        AnalyticsCard card = new AnalyticsCard(title, value, trend, "");
        card.setIcon(icon);
        card.setColor(color);
        return card;
    }

    private String calculateTrend(double current, double previous) {
        if (previous == 0) return "stable";
        double change = ((current - previous) / previous) * 100;
        if (change > 5) return "up";
        if (change < -5) return "down";
        return "stable";
    }
    private List<Map<String, Object>> getEquipmentTypeBreakdown(Location location) {
        List<Equipment> equipment = location != null ? 
            equipmentRepository.findByLocation(location) :
            equipmentRepository.findAll();
        
        return equipment.stream()
            .map(eq -> {
                Map<String, Object> data = new LinkedHashMap<>();
                data.put("equipmentName", eq.getName());
                data.put("totalUnits", equipmentUnitRepository.countByEquipment(eq));
                data.put("availableUnits", equipmentUnitRepository.countByEquipmentAndStatus(
                    eq, EquipmentUnit.UnitStatus.AVAILABLE));
                data.put("assignedUnits", equipmentUnitRepository.countByEquipmentAndStatus(
                    eq, EquipmentUnit.UnitStatus.ASSIGNED));
                data.put("maintenanceUnits", equipmentUnitRepository.countByEquipmentAndStatus(
                    eq, EquipmentUnit.UnitStatus.MAINTENANCE));
                data.put("damagedUnits", equipmentUnitRepository.countByEquipmentAndStatus(
                    eq, EquipmentUnit.UnitStatus.DAMAGED));
                return data;
            })
            .collect(Collectors.toList());
    }
}