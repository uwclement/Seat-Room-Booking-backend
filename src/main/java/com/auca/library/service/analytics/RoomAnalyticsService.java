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
import com.auca.library.dto.response.analytics.RoomAnalyticsSummary;
import com.auca.library.dto.response.analytics.RoomChartsData;
import com.auca.library.model.Location;
import com.auca.library.model.Room;
import com.auca.library.model.RoomBooking;
import com.auca.library.repository.RoomBookingRepository;
import com.auca.library.repository.RoomRepository;

@Service
public class RoomAnalyticsService {

    @Autowired
    private RoomRepository roomRepository;
    
    @Autowired
    private RoomBookingRepository roomBookingRepository;
    
    @Autowired
    private AnalyticsReportService reportService;

    public RoomAnalyticsSummary getSummary(AnalyticsFilterRequest filter) {
        Location location = parseLocation(filter.getLocation());
        LocalDateTime[] dateRange = parseDateRange(filter);
        
        RoomAnalyticsSummary summary = new RoomAnalyticsSummary(
            location != null ? location.name() : "ALL", 
            formatDateRange(dateRange[0], dateRange[1])
        );
        
        List<AnalyticsCard> cards = new ArrayList<>();
        
        // Card 1: Total Rooms
        int totalRooms = getTotalRooms();
        cards.add(createAnalyticsCard("Total Rooms", String.valueOf(totalRooms), 
            "stable", "🏢", "blue"));
        
        // Card 2: Active Bookings
        int activeBookings = getActiveBookings(location);
        int previousActiveBookings = getPreviousActiveBookings(location, dateRange);
        cards.add(createAnalyticsCard("Active Bookings", String.valueOf(activeBookings),
            calculateTrend(activeBookings, previousActiveBookings), "📅", "green"));
        
        // Card 3: Approval Rate
        double approvalRate = getApprovalRate(location, dateRange);
        double previousApprovalRate = getPreviousApprovalRate(location, dateRange);
        cards.add(createAnalyticsCard("Approval Rate", String.format("%.0f%%", approvalRate),
            calculateTrend(approvalRate, previousApprovalRate), "✅", "orange"));
        
        // Card 4: Average Capacity
        double avgCapacity = getAverageCapacityUtilization(location, dateRange);
        double previousAvgCapacity = getPreviousAverageCapacity(location, dateRange);
        cards.add(createAnalyticsCard("Avg Capacity", String.format("%.0f%%", avgCapacity),
            calculateTrend(avgCapacity, previousAvgCapacity), "👥", "purple"));
        
        summary.setSummaryCards(cards);
        return summary;
    }

    public RoomChartsData getChartsData(AnalyticsFilterRequest filter) {
        Location location = parseLocation(filter.getLocation());
        LocalDateTime[] dateRange = parseDateRange(filter);
        
        RoomChartsData chartsData = new RoomChartsData();
        
        // Chart 1: Booking Status Distribution
        chartsData.setBookingStatusChart(getBookingStatusChart(location, dateRange));
        
        // Chart 2: Room Utilization by Category
        chartsData.setRoomUtilizationChart(getRoomUtilizationChart(location, dateRange));
        
        // Chart 3: Monthly Booking Trends
        chartsData.setMonthlyTrendsChart(getMonthlyTrendsChart(location, dateRange));
        
        return chartsData;
    }

    public byte[] generateSimpleReport(AnalyticsFilterRequest filter) {
        RoomAnalyticsSummary summary = getSummary(filter);
        RoomChartsData charts = getChartsData(filter);
        return reportService.generateRoomSimpleReport(summary, charts);
    }

    public byte[] generateDetailedReport(AnalyticsFilterRequest filter) {
        RoomAnalyticsSummary summary = getSummary(filter);
        RoomChartsData charts = getChartsData(filter);
        
        // Additional detailed data
        Location location = parseLocation(filter.getLocation());
        LocalDateTime[] dateRange = parseDateRange(filter);
        location = Location.GISHUSHU;
        
        List<Map<String, Object>> topBookedRooms = getTopBookedRooms(location, dateRange, 10);
        List<Map<String, Object>> underutilizedRooms = getUnderutilizedRooms(location, dateRange, 10);
        List<Map<String, Object>> equipmentRequests = getEquipmentRequestSummary(location, dateRange);
        List<Map<String, Object>> rejectedBookings = getRejectedBookingSummary(location, dateRange);
        
        return reportService.generateRoomDetailedReport(summary, charts, 
            topBookedRooms, underutilizedRooms, equipmentRequests, rejectedBookings);
    }

    // ===== PRIVATE HELPER METHODS =====

    private int getTotalRooms() {
        return (int) roomRepository.count();
    }

    private int getActiveBookings(Location location) {
        LocalDateTime now = LocalDateTime.now();
        if (location != null) {
            return roomBookingRepository.countActiveBookingsByLocation(location, now);
        }
        return roomBookingRepository.countActiveBookings(now);
    }

    private double getApprovalRate(Location location, LocalDateTime[] dateRange) {
        List<RoomBooking> bookings = getBookingsInRange(location, dateRange);
        
        if (bookings.isEmpty()) return 0.0;
        
        long totalRequiringApproval = bookings.stream()
            .filter(RoomBooking::isRequiresApproval)
            .count();
        
        if (totalRequiringApproval == 0) return 100.0; // All auto-approved
        
        long approvedBookings = bookings.stream()
            .filter(booking -> booking.getStatus() == RoomBooking.BookingStatus.CONFIRMED)
            .count();
        
        return (double) approvedBookings / totalRequiringApproval * 100;
    }

    private double getAverageCapacityUtilization(Location location, LocalDateTime[] dateRange) {
        List<RoomBooking> bookings = getBookingsInRange(location, dateRange);
        
        if (bookings.isEmpty()) return 0.0;
        
        double totalUtilization = bookings.stream()
            .filter(booking -> booking.getStatus() == RoomBooking.BookingStatus.CONFIRMED)
            .mapToDouble(booking -> {
                int roomCapacity = booking.getRoom().getCapacity();
                int participantCount = booking.getParticipants().size() + 1; // +1 for organizer
                return roomCapacity > 0 ? (double) participantCount / roomCapacity * 100 : 0;
            })
            .sum();
        
        return totalUtilization / bookings.size();
    }

    private ChartData getBookingStatusChart(Location location, LocalDateTime[] dateRange) {
        List<RoomBooking> bookings = getBookingsInRange(location, dateRange);
        
        Map<String, Long> statusDistribution = bookings.stream()
            .collect(Collectors.groupingBy(
                booking -> booking.getStatus().toString(),
                Collectors.counting()));
        
        List<String> labels = new ArrayList<>(statusDistribution.keySet());
        List<Number> data = new ArrayList<>(statusDistribution.values());
        
        return new ChartData("pie", "Booking Status Distribution", labels, data);
    }

    private ChartData getRoomUtilizationChart(Location location, LocalDateTime[] dateRange) {
        List<RoomBooking> bookings = getBookingsInRange(location, dateRange);
        
        Map<String, Long> categoryUtilization = bookings.stream()
            .filter(booking -> booking.getStatus() == RoomBooking.BookingStatus.CONFIRMED)
            .collect(Collectors.groupingBy(
                booking -> booking.getRoom().getCategory().toString(),
                Collectors.counting()));
        
        List<String> labels = new ArrayList<>(categoryUtilization.keySet());
        List<Number> data = new ArrayList<>(categoryUtilization.values());
        
        return new ChartData("bar", "Room Utilization by Category", labels, data);
    }

    private ChartData getMonthlyTrendsChart(Location location, LocalDateTime[] dateRange) {
        List<RoomBooking> bookings = getBookingsInRange(location, dateRange);
        
        Map<String, Long> monthlyBookings = bookings.stream()
            .collect(Collectors.groupingBy(
                booking -> booking.getStartTime().format(DateTimeFormatter.ofPattern("MMM yyyy")),
                LinkedHashMap::new,
                Collectors.counting()));
        
        List<String> labels = new ArrayList<>(monthlyBookings.keySet());
        List<Number> data = new ArrayList<>(monthlyBookings.values());
        
        return new ChartData("line", "Monthly Booking Trends", labels, data);
    }

    private List<RoomBooking> getBookingsInRange(Location location, LocalDateTime[] dateRange) {
        if (location != null) {
            return roomBookingRepository.findByRoomLocationAndTimeRange(
                location, dateRange[0], dateRange[1]);
        }
        return roomBookingRepository.findByStartTimeBetween(dateRange[0], dateRange[1]);
    }

    private List<Map<String, Object>> getTopBookedRooms(Location location, 
            LocalDateTime[] dateRange, int limit) {
        List<RoomBooking> bookings = getBookingsInRange(location, dateRange);
        
        Map<Long, Long> roomBookingCounts = bookings.stream()
            .collect(Collectors.groupingBy(
                booking -> booking.getRoom().getId(),
                Collectors.counting()));
        
        return roomBookingCounts.entrySet().stream()
            .sorted(Map.Entry.<Long, Long>comparingByValue().reversed())
            .limit(limit)
            .map(entry -> {
                Room room = roomRepository.findById(entry.getKey()).orElse(null);
                Map<String, Object> roomData = new LinkedHashMap<>();
                roomData.put("roomNumber", room != null ? room.getRoomNumber() : "Unknown");
                roomData.put("roomName", room != null ? room.getName() : "Unknown");
                roomData.put("bookingCount", entry.getValue());
                roomData.put("category", room != null ? room.getCategory().toString() : "Unknown");
                roomData.put("capacity", room != null ? room.getCapacity() : 0);
                return roomData;
            })
            .collect(Collectors.toList());
    }

    private List<Map<String, Object>> getUnderutilizedRooms( Location location, LocalDateTime[] dateRange, int limit) {
        List<Room> rooms = roomRepository.findAll();
        location = Location.GISHUSHU;
        
        List<RoomBooking> bookings = getBookingsInRange(location, dateRange);
        Map<Long, Long> roomBookingCounts = bookings.stream()
            .collect(Collectors.groupingBy(
                booking -> booking.getRoom().getId(),
                Collectors.counting()));
        
        return rooms.stream()
            .filter(Room::isAvailable)
            .map(room -> {
                Map<String, Object> roomData = new LinkedHashMap<>();
                roomData.put("roomNumber", room.getRoomNumber());
                roomData.put("roomName", room.getName());
                roomData.put("bookingCount", roomBookingCounts.getOrDefault(room.getId(), 0L));
                roomData.put("category", room.getCategory().toString());
                roomData.put("capacity", room.getCapacity());
                return roomData;
            })
            .sorted((a, b) -> Long.compare((Long) a.get("bookingCount"), (Long) b.get("bookingCount")))
            .limit(limit)
            .collect(Collectors.toList());
    }

    private List<Map<String, Object>> getEquipmentRequestSummary(Location location, LocalDateTime[] dateRange) {
        List<RoomBooking> bookings = getBookingsInRange(location, dateRange);
        
        Map<String, Long> equipmentRequests = bookings.stream()
            .filter(booking -> !booking.getRequestedEquipment().isEmpty())
            .flatMap(booking -> booking.getRequestedEquipment().stream())
            .collect(Collectors.groupingBy(
                equipment -> equipment.getName(),
                Collectors.counting()));
        
        return equipmentRequests.entrySet().stream()
            .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
            .limit(10)
            .map(entry -> {
                Map<String, Object> data = new LinkedHashMap<>();
                data.put("equipmentName", entry.getKey());
                data.put("requestCount", entry.getValue());
                return data;
            })
            .collect(Collectors.toList());
    }

    private List<Map<String, Object>> getRejectedBookingSummary(Location location, LocalDateTime[] dateRange) {
        List<RoomBooking> rejectedBookings = getBookingsInRange(location, dateRange).stream()
            .filter(booking -> booking.getStatus() == RoomBooking.BookingStatus.REJECTED)
            .collect(Collectors.toList());
        
        Map<String, Long> rejectionReasons = rejectedBookings.stream()
            .filter(booking -> booking.getRejectionReason() != null)
            .collect(Collectors.groupingBy(
                RoomBooking::getRejectionReason,
                Collectors.counting()));
        
        return rejectionReasons.entrySet().stream()
            .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
            .map(entry -> {
                Map<String, Object> data = new LinkedHashMap<>();
                data.put("reason", entry.getKey());
                data.put("count", entry.getValue());
                return data;
            })
            .collect(Collectors.toList());
    }

    // ===== PREVIOUS PERIOD COMPARISON METHODS =====

    private int getPreviousActiveBookings(Location location, LocalDateTime[] dateRange) {
        long periodDays = ChronoUnit.DAYS.between(dateRange[0], dateRange[1]);
        LocalDateTime prevTime = LocalDateTime.now().minusDays(periodDays);
        
        if (location != null) {
            return roomBookingRepository.countActiveBookingsByLocation(location, prevTime);
        }
        return roomBookingRepository.countActiveBookings(prevTime);
    }

    private double getPreviousApprovalRate(Location location, LocalDateTime[] dateRange) {
        long periodDays = ChronoUnit.DAYS.between(dateRange[0], dateRange[1]);
        LocalDateTime[] prevRange = {
            dateRange[0].minusDays(periodDays),
            dateRange[0]
        };
        return getApprovalRate(location, prevRange);
    }

    private double getPreviousAverageCapacity(Location location, LocalDateTime[] dateRange) {
        long periodDays = ChronoUnit.DAYS.between(dateRange[0], dateRange[1]);
        LocalDateTime[] prevRange = {
            dateRange[0].minusDays(periodDays),
            dateRange[0]
        };
        return getAverageCapacityUtilization(location, prevRange);
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
}