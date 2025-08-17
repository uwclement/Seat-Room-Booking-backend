package com.auca.library.service.analytics;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.auca.library.dto.response.analytics.AnalyticsCard;
import com.auca.library.dto.response.analytics.AnalyticsFilterRequest;
import com.auca.library.dto.response.analytics.ChartData;
import com.auca.library.dto.response.analytics.SeatAnalyticsSummary;
import com.auca.library.dto.response.analytics.SeatChartsData;
import com.auca.library.model.Booking;
import com.auca.library.model.Location;
import com.auca.library.model.Seat;
import com.auca.library.repository.BookingRepository;
import com.auca.library.repository.SeatRepository;
import com.auca.library.repository.WaitListRepository;

@Service
public class SeatAnalyticsService {

    @Autowired
    private SeatRepository seatRepository;
    
    @Autowired
    private BookingRepository bookingRepository;
    
    @Autowired
    private WaitListRepository waitListRepository;
    
    @Autowired
    private AnalyticsReportService reportService;

    public SeatAnalyticsSummary getSummary(AnalyticsFilterRequest filter) {
        Location location = parseLocation(filter.getLocation());
        LocalDateTime[] dateRange = parseDateRange(filter);
        
        SeatAnalyticsSummary summary = new SeatAnalyticsSummary(
            location != null ? location.name() : "ALL", 
            formatDateRange(dateRange[0], dateRange[1])
        );
        
        List<AnalyticsCard> cards = new ArrayList<>();
        
        // Card 1: Total Seats
        int totalSeats = getTotalSeats(location);
        int previousTotalSeats = getPreviousPeriodTotalSeats(location, dateRange);
        cards.add(createAnalyticsCard("Total Seats", String.valueOf(totalSeats), 
            calculateTrend(totalSeats, previousTotalSeats), "🪑", "blue"));
        
        // Card 2: Available Now
        int availableNow = getAvailableSeatsNow(location);
        int previousAvailable = getPreviousAvailableSeats(location, dateRange);
        cards.add(createAnalyticsCard("Available Now", String.valueOf(availableNow),
            calculateTrend(availableNow, previousAvailable), "✅", "green"));
        
        // Card 3: Peak Usage
        double peakUsage = getPeakUsagePercentage(location, dateRange);
        double previousPeakUsage = getPreviousPeakUsage(location, dateRange);
        cards.add(createAnalyticsCard("Peak Usage", String.format("%.1f%%", peakUsage),
            calculateTrend(peakUsage, previousPeakUsage), "📈", "orange"));
        
        // Card 4: Average Duration
        double avgDuration = getAverageBookingDuration(location, dateRange);
        double previousAvgDuration = getPreviousAverageDuration(location, dateRange);
        cards.add(createAnalyticsCard("Avg Duration", String.format("%.1fh", avgDuration),
            calculateTrend(avgDuration, previousAvgDuration), "⏱️", "purple"));
        
        summary.setSummaryCards(cards);
        return summary;
    }

    public SeatChartsData getChartsData(AnalyticsFilterRequest filter) {
        Location location = parseLocation(filter.getLocation());
        LocalDateTime[] dateRange = parseDateRange(filter);
        
        SeatChartsData chartsData = new SeatChartsData();
        
        // Chart 1: Hourly Usage Pattern
        chartsData.setHourlyUsageChart(getHourlyUsageChart(location, dateRange));
        
        // Chart 2: Zone Distribution
        chartsData.setZoneDistributionChart(getZoneDistributionChart(location));
        
        // Chart 3: Weekly Trend
        chartsData.setWeeklyTrendChart(getWeeklyTrendChart(location, dateRange));
        
        return chartsData;
    }

    public byte[] generateSimpleReport(AnalyticsFilterRequest filter) {
        SeatAnalyticsSummary summary = getSummary(filter);
        SeatChartsData charts = getChartsData(filter);
        return reportService.generateSeatSimpleReport(summary, charts);
    }

    public byte[] generateDetailedReport(AnalyticsFilterRequest filter) {
        SeatAnalyticsSummary summary = getSummary(filter);
        SeatChartsData charts = getChartsData(filter);
        
        // Additional detailed data
        Location location = parseLocation(filter.getLocation());
        LocalDateTime[] dateRange = parseDateRange(filter);
        
        List<Map<String, Object>> topPerformingSeats = getTopPerformingSeats(location, dateRange, 10);
        List<Map<String, Object>> underutilizedSeats = getUnderutilizedSeats(location, dateRange, 10);
        List<Map<String, Object>> maintenanceSeats = getMaintenanceRequiredSeats(location);
        
        return reportService.generateSeatDetailedReport(summary, charts, 
            topPerformingSeats, underutilizedSeats, maintenanceSeats);
    }

    // ===== PRIVATE HELPER METHODS =====

    private int getTotalSeats(Location location) {
        if (location != null) {
            return (int) seatRepository.countByLocation(location);
        }
        return (int) seatRepository.count();
    }

    private int getAvailableSeatsNow(Location location) {
        LocalDateTime now = LocalDateTime.now();
        List<Seat> seats = location != null ? 
            seatRepository.findByLocationAndIsDisabledFalse(location) :
            seatRepository.findByIsDisabled(false);
        
        return (int) seats.stream()
            .filter(seat -> isSeatAvailableNow(seat, now))
            .count();
    }

    private boolean isSeatAvailableNow(Seat seat, LocalDateTime now) {
        List<Booking> activeBookings = bookingRepository.findActiveBySeatAndTime(
            seat.getId(), now, now.plusMinutes(1));
        return activeBookings.isEmpty();
    }

    private double getPeakUsagePercentage(Location location, LocalDateTime[] dateRange) {
        List<Seat> seats = location != null ? 
            seatRepository.findByLocation(location) :
            seatRepository.findAll();
        
        if (seats.isEmpty()) return 0.0;
        
        // Find peak hour in the date range
        Map<Integer, Long> hourlyUsage = getHourlyUsageMap(location, dateRange);
        long maxUsage = hourlyUsage.values().stream().mapToLong(Long::longValue).max().orElse(0L);
        
        return (double) maxUsage / seats.size() * 100;
    }

    private double getAverageBookingDuration(Location location, LocalDateTime[] dateRange) {
        List<Booking> bookings = getBookingsInRange(location, dateRange);
        
        if (bookings.isEmpty()) return 0.0;
        
        double totalHours = bookings.stream()
            .mapToDouble(booking -> ChronoUnit.MINUTES.between(
                booking.getStartTime(), booking.getEndTime()) / 60.0)
            .sum();
        
        return totalHours / bookings.size();
    }

    private ChartData getHourlyUsageChart(Location location, LocalDateTime[] dateRange) {
        Map<Integer, Long> hourlyUsage = getHourlyUsageMap(location, dateRange);
        
        List<String> labels = IntStream.range(8, 22)  // 8 AM to 10 PM
            .mapToObj(hour -> String.format("%02d:00", hour))
            .collect(Collectors.toList());
        
        List<Number> data = IntStream.range(8, 22)
            .mapToObj(hour -> hourlyUsage.getOrDefault(hour, 0L))
            .collect(Collectors.toList());
        
        return new ChartData("line", "Hourly Usage Pattern", labels, data);
    }

    private ChartData getZoneDistributionChart(Location location) {
        Map<String, Long> zoneDistribution = getZoneDistribution(location);
        
        List<String> labels = new ArrayList<>(zoneDistribution.keySet());
        List<Number> data = new ArrayList<>(zoneDistribution.values());
        
        return new ChartData("pie", "Zone Type Distribution", labels, data);
    }

    private ChartData getWeeklyTrendChart(Location location, LocalDateTime[] dateRange) {
        Map<DayOfWeek, Long> weeklyUsage = getWeeklyUsageMap(location, dateRange);
        
        List<String> labels = Arrays.asList("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun");
        List<Number> data = Arrays.asList(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
            DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)
            .stream()
            .map(day -> weeklyUsage.getOrDefault(day, 0L))
            .collect(Collectors.toList());
        
        return new ChartData("bar", "Weekly Usage Trends", labels, data);
    }

    private Map<Integer, Long> getHourlyUsageMap(Location location, LocalDateTime[] dateRange) {
        List<Booking> bookings = getBookingsInRange(location, dateRange);
        
        Map<Integer, Long> hourlyUsage = new LinkedHashMap<>();
        for (int hour = 8; hour <= 21; hour++) {
            hourlyUsage.put(hour, 0L);
        }
        
        for (Booking booking : bookings) {
            int startHour = booking.getStartTime().getHour();
            int endHour = booking.getEndTime().getHour();
            
            for (int hour = startHour; hour < endHour && hour <= 21; hour++) {
                if (hour >= 8) {
                    hourlyUsage.put(hour, hourlyUsage.get(hour) + 1);
                }
            }
        }
        
        return hourlyUsage;
    }

    private Map<String, Long> getZoneDistribution(Location location) {
        List<Seat> seats = location != null ? 
            seatRepository.findByLocation(location) :
            seatRepository.findAll();
        
        return seats.stream()
            .collect(Collectors.groupingBy(
                seat -> seat.getZoneType().toString(),
                Collectors.counting()));
    }

    private Map<DayOfWeek, Long> getWeeklyUsageMap(Location location, LocalDateTime[] dateRange) {
        List<Booking> bookings = getBookingsInRange(location, dateRange);
        
        return bookings.stream()
            .collect(Collectors.groupingBy(
                booking -> booking.getStartTime().getDayOfWeek(),
                Collectors.counting()));
    }

    private List<Booking> getBookingsInRange(Location location, LocalDateTime[] dateRange) {
        if (location != null) {
            return bookingRepository.findBySeatLocationAndTimeRange(
                location, dateRange[0], dateRange[1]);
        }
        return bookingRepository.findByTimeRange(dateRange[0], dateRange[1]);
    }

    private List<Map<String, Object>> getTopPerformingSeats(Location location, 
            LocalDateTime[] dateRange, int limit) {
        List<Booking> bookings = getBookingsInRange(location, dateRange);
        
        Map<Long, Long> seatBookingCounts = bookings.stream()
            .collect(Collectors.groupingBy(
                booking -> booking.getSeat().getId(),
                Collectors.counting()));
        
        return seatBookingCounts.entrySet().stream()
            .sorted(Map.Entry.<Long, Long>comparingByValue().reversed())
            .limit(limit)
            .map(entry -> {
                Seat seat = seatRepository.findById(entry.getKey()).orElse(null);
                Map<String, Object> seatData = new LinkedHashMap<>();
                seatData.put("seatNumber", seat != null ? seat.getSeatNumber() : "Unknown");
                seatData.put("bookingCount", entry.getValue());
                seatData.put("zoneType", seat != null ? seat.getZoneType().toString() : "Unknown");
                return seatData;
            })
            .collect(Collectors.toList());
    }

    private List<Map<String, Object>> getUnderutilizedSeats(Location location, 
            LocalDateTime[] dateRange, int limit) {
        List<Seat> seats = location != null ? 
            seatRepository.findByLocation(location) :
            seatRepository.findAll();
        
        List<Booking> bookings = getBookingsInRange(location, dateRange);
        Map<Long, Long> seatBookingCounts = bookings.stream()
            .collect(Collectors.groupingBy(
                booking -> booking.getSeat().getId(),
                Collectors.counting()));
        
        return seats.stream()
            .filter(seat -> !seat.isDisabled())
            .map(seat -> {
                Map<String, Object> seatData = new LinkedHashMap<>();
                seatData.put("seatNumber", seat.getSeatNumber());
                seatData.put("bookingCount", seatBookingCounts.getOrDefault(seat.getId(), 0L));
                seatData.put("zoneType", seat.getZoneType().toString());
                return seatData;
            })
            .sorted((a, b) -> Long.compare((Long) a.get("bookingCount"), (Long) b.get("bookingCount")))
            .limit(limit)
            .collect(Collectors.toList());
    }

    private List<Map<String, Object>> getMaintenanceRequiredSeats(Location location) {
        List<Seat> seats = location != null ? 
            seatRepository.findByLocationAndIsDisabledTrue(location) :
            seatRepository.findByIsDisabled(true);
        
        return seats.stream()
            .map(seat -> {
                Map<String, Object> seatData = new LinkedHashMap<>();
                seatData.put("seatNumber", seat.getSeatNumber());
                seatData.put("zoneType", seat.getZoneType().toString());
                seatData.put("hasDesktop", seat.isHasDesktop());
                seatData.put("description", seat.getDescription());
                return seatData;
            })
            .collect(Collectors.toList());
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

    // Previous period comparison methods
    private int getPreviousPeriodTotalSeats(Location location, LocalDateTime[] dateRange) {
        // For simplicity, assume seats don't change much over time
        return getTotalSeats(location);
    }

    private int getPreviousAvailableSeats(Location location, LocalDateTime[] dateRange) {
        // Calculate average available seats in previous period
        long periodDays = ChronoUnit.DAYS.between(dateRange[0], dateRange[1]);
        LocalDateTime prevStart = dateRange[0].minusDays(periodDays);
        LocalDateTime prevEnd = dateRange[0];
        
        // Simplified calculation - in reality, you'd want more sophisticated logic
        return getAvailableSeatsNow(location);
    }

    private double getPreviousPeakUsage(Location location, LocalDateTime[] dateRange) {
        long periodDays = ChronoUnit.DAYS.between(dateRange[0], dateRange[1]);
        LocalDateTime[] prevRange = {
            dateRange[0].minusDays(periodDays),
            dateRange[0]
        };
        return getPeakUsagePercentage(location, prevRange);
    }

    private double getPreviousAverageDuration(Location location, LocalDateTime[] dateRange) {
        long periodDays = ChronoUnit.DAYS.between(dateRange[0], dateRange[1]);
        LocalDateTime[] prevRange = {
            dateRange[0].minusDays(periodDays),
            dateRange[0]
        };
        return getAverageBookingDuration(location, prevRange);
    }
}