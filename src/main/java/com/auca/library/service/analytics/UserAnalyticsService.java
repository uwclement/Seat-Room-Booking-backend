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
import com.auca.library.dto.response.analytics.UserAnalyticsSummary;
import com.auca.library.dto.response.analytics.UserChartsData;
import com.auca.library.model.Booking;
import com.auca.library.model.EquipmentRequest;
import com.auca.library.model.Location;
import com.auca.library.model.RoomBooking;
import com.auca.library.model.User;
import com.auca.library.repository.BookingRepository;
import com.auca.library.repository.EquipmentRequestRepository;
import com.auca.library.repository.RoomBookingRepository;
import com.auca.library.repository.UserRepository;

@Service
public class UserAnalyticsService {

    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private BookingRepository bookingRepository;
    
    @Autowired
    private RoomBookingRepository roomBookingRepository;
    
    @Autowired
    private EquipmentRequestRepository equipmentRequestRepository;
    
    @Autowired
    private AnalyticsReportService reportService;

    public UserAnalyticsSummary getSummary(AnalyticsFilterRequest filter) {
        Location location = parseLocation(filter.getLocation());
        LocalDateTime[] dateRange = parseDateRange(filter);
        
        UserAnalyticsSummary summary = new UserAnalyticsSummary(
            location != null ? location.name() : "ALL", 
            formatDateRange(dateRange[0], dateRange[1])
        );
        
        List<AnalyticsCard> cards = new ArrayList<>();
        
        // Card 1: Total Users
        int totalUsers = getTotalUsers(location);
        int previousTotalUsers = getPreviousTotalUsers(location, dateRange);
        cards.add(createAnalyticsCard("Total Users", String.valueOf(totalUsers), 
            calculateTrend(totalUsers, previousTotalUsers), "👥", "blue"));
        
        // Card 2: Active Today
        int activeToday = getActiveUsersToday(location);
        int previousActiveToday = getPreviousActiveToday(location, dateRange);
        cards.add(createAnalyticsCard("Active Today", String.valueOf(activeToday),
            calculateTrend(activeToday, previousActiveToday), "🟢", "green"));
        
        // Card 3: New This Month
        int newThisMonth = getNewUsersThisMonth(location);
        int previousNewMonth = getPreviousNewMonth(location, dateRange);
        cards.add(createAnalyticsCard("New This Month", String.valueOf(newThisMonth),
            calculateTrend(newThisMonth, previousNewMonth), "✨", "orange"));
        
        // Card 4: Staff Users
        int staffUsers = getStaffUsers(location);
        cards.add(createAnalyticsCard("Staff", String.valueOf(staffUsers),
            "stable", "👔", "purple"));
        
        summary.setSummaryCards(cards);
        return summary;
    }

    public UserChartsData getChartsData(AnalyticsFilterRequest filter) {
        Location location = parseLocation(filter.getLocation());
        LocalDateTime[] dateRange = parseDateRange(filter);
        
        UserChartsData chartsData = new UserChartsData();
        
        // Chart 1: User Type Distribution
        chartsData.setUserTypeChart(getUserTypeChart(location));
        
        // Chart 2: Daily Active Users
        chartsData.setDailyActiveChart(getDailyActiveChart(location, dateRange));
        
        // Chart 3: Department Usage (for staff/professors)
        chartsData.setDepartmentUsageChart(getDepartmentUsageChart(location, dateRange));
        
        return chartsData;
    }

    public byte[] generateSimpleReport(AnalyticsFilterRequest filter) {
        UserAnalyticsSummary summary = getSummary(filter);
        UserChartsData charts = getChartsData(filter);
        return reportService.generateUserSimpleReport(summary, charts);
    }

    public byte[] generateDetailedReport(AnalyticsFilterRequest filter) {
        UserAnalyticsSummary summary = getSummary(filter);
        UserChartsData charts = getChartsData(filter);
        
        // Additional detailed data
        Location location = parseLocation(filter.getLocation());
        LocalDateTime[] dateRange = parseDateRange(filter);
        
        List<Map<String, Object>> userActivityBreakdown = getUserActivityBreakdown(location, dateRange);
        List<Map<String, Object>> topActiveUsers = getTopActiveUsers(location, dateRange, 20);
        List<Map<String, Object>> professorApprovalStatus = getProfessorApprovalStatus(location);
        List<Map<String, Object>> librarianWorkload = getLibrarianWorkload(location, dateRange);
        List<Map<String, Object>> newUserTrends = getNewUserTrends(location, dateRange);
        
        return reportService.generateUserDetailedReport(summary, charts, 
            userActivityBreakdown, topActiveUsers, professorApprovalStatus, 
            librarianWorkload, newUserTrends);
    }

    // ===== PRIVATE HELPER METHODS =====

    private int getTotalUsers(Location location) {
        if (location != null) {
            return userRepository.countByLocation(location);
        }
        return (int) userRepository.count();
    }

    private int getActiveUsersToday(Location location) {
        LocalDateTime today = LocalDateTime.now().toLocalDate().atStartOfDay();
        LocalDateTime now = LocalDateTime.now();
        
        // Users who have any activity today (bookings, room bookings, equipment requests)
        List<Long> activeUserIds = new ArrayList<>();
        
        // From seat bookings
        List<Booking> todayBookings = location != null ? 
            bookingRepository.findBySeatLocationAndTimeRange(location, today, now) :
            bookingRepository.findByTimeRange(today, now);
        activeUserIds.addAll(todayBookings.stream()
            .map(booking -> booking.getUser().getId())
            .collect(Collectors.toList()));
        
        // From room bookings
        List<RoomBooking> todayRoomBookings = location != null ? 
            roomBookingRepository.findByRoomLocationAndTimeRange(location, today, now) :
            roomBookingRepository.findByStartTimeBetween(today, now);
        activeUserIds.addAll(todayRoomBookings.stream()
            .map(booking -> booking.getUser().getId())
            .collect(Collectors.toList()));
        
        // From equipment requests
        List<EquipmentRequest> todayEquipmentRequests = location != null ? 
            equipmentRequestRepository.findByEquipmentLocationAndDateRange(location, today, now) :
            equipmentRequestRepository.findByStartTimeBetween(today, now);
        activeUserIds.addAll(todayEquipmentRequests.stream()
            .map(request -> request.getUser().getId())
            .collect(Collectors.toList()));
        
        return (int) activeUserIds.stream().distinct().count();
    }

    private int getNewUsersThisMonth(Location location) {
        LocalDateTime monthStart = LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
        LocalDateTime now = LocalDateTime.now();
        
        if (location != null) {
            return userRepository.countByLocationAndCreatedAtBetween(location, monthStart, now);
        }
        return userRepository.countByCreatedAtBetween(monthStart, now);
    }

    private int getStaffUsers(Location location) {
        if (location != null) {
            return userRepository.countStaffByLocation(location);
        }
        return userRepository.countAllStaff();
    }

    private ChartData getUserTypeChart(Location location) {
        Map<String, Long> userTypeDistribution = new LinkedHashMap<>();
        
        if (location != null) {
            userTypeDistribution.put("Students", (long) userRepository.countStudentsByLocation(location));
            userTypeDistribution.put("Professors", (long) userRepository.countProfessorsByLocation(location));
            userTypeDistribution.put("Librarians", (long) userRepository.countLibrariansByLocation(location));
            userTypeDistribution.put("Admins", (long) userRepository.countAdminsByLocation(location));
            userTypeDistribution.put("Equipment Admins", (long) userRepository.countEquipmentAdminsByLocation(location));
        } else {
            userTypeDistribution.put("Students", (long) userRepository.countAllStudents());
            userTypeDistribution.put("Professors", (long) userRepository.countAllProfessors());
            userTypeDistribution.put("Librarians", (long) userRepository.countAllLibrarians());
            userTypeDistribution.put("Admins", (long) userRepository.countAllAdmins());
            userTypeDistribution.put("Equipment Admins", (long) userRepository.countAllEquipmentAdmins());
        }
        
        List<String> labels = new ArrayList<>(userTypeDistribution.keySet());
        List<Number> data = new ArrayList<>(userTypeDistribution.values());
        
        return new ChartData("pie", "User Type Distribution", labels, data);
    }

    private ChartData getDailyActiveChart(Location location, LocalDateTime[] dateRange) {
        Map<String, Long> dailyActivity = new LinkedHashMap<>();
        
        // Generate daily activity for the last 7 days
        for (int i = 6; i >= 0; i--) {
            LocalDateTime dayStart = LocalDateTime.now().minusDays(i).toLocalDate().atStartOfDay();
            LocalDateTime dayEnd = dayStart.plusDays(1).minusSeconds(1);
            
            List<Long> activeUserIds = new ArrayList<>();
            
            // Collect active users from all activity types
            List<Booking> dayBookings = location != null ? 
                bookingRepository.findBySeatLocationAndTimeRange(location, dayStart, dayEnd) :
                bookingRepository.findByTimeRange(dayStart, dayEnd);
            activeUserIds.addAll(dayBookings.stream()
                .map(booking -> booking.getUser().getId())
                .collect(Collectors.toList()));
            
            List<RoomBooking> dayRoomBookings = location != null ? 
                roomBookingRepository.findByRoomLocationAndTimeRange(location, dayStart, dayEnd) :
                roomBookingRepository.findByStartTimeBetween(dayStart, dayEnd);
            activeUserIds.addAll(dayRoomBookings.stream()
                .map(booking -> booking.getUser().getId())
                .collect(Collectors.toList()));
            
            String dayLabel = dayStart.format(DateTimeFormatter.ofPattern("MMM dd"));
            long uniqueActiveUsers = activeUserIds.stream().distinct().count();
            dailyActivity.put(dayLabel, uniqueActiveUsers);
        }
        
        List<String> labels = new ArrayList<>(dailyActivity.keySet());
        List<Number> data = new ArrayList<>(dailyActivity.values());
        
        return new ChartData("line", "Daily Active Users (Last 7 Days)", labels, data);
    }

    private ChartData getDepartmentUsageChart(Location location, LocalDateTime[] dateRange) {
        // Get professor/staff activity by department (simplified)
        List<User> staffUsers = location != null ? 
            userRepository.findStaffByLocation(location) :
            userRepository.findAllStaff();
        
        Map<String, Long> departmentActivity = new LinkedHashMap<>();
        
        for (User user : staffUsers) {
            String department = user.getRoles() != null ? user.getFullName() : "Unknown";
            
            // Count their activities in the date range
            long userActivity = 0;
            
            List<Booking> userBookings = bookingRepository.findByUserAndTimeRange(
                user, dateRange[0], dateRange[1]);
            userActivity += userBookings.size();
            
            List<RoomBooking> userRoomBookings = roomBookingRepository.findByUserAndTimeRange(
                user, dateRange[0], dateRange[1]);
            userActivity += userRoomBookings.size();
            
            List<EquipmentRequest> userEquipmentRequests = equipmentRequestRepository.findByUserAndTimeRange(
                user, dateRange[0], dateRange[1]);
            userActivity += userEquipmentRequests.size();
            
            departmentActivity.put(department, 
                departmentActivity.getOrDefault(department, 0L) + userActivity);
        }
        
        List<String> labels = new ArrayList<>(departmentActivity.keySet());
        List<Number> data = new ArrayList<>(departmentActivity.values());
        
        return new ChartData("bar", "Department Usage Activity", labels, data);
    }

    private List<Map<String, Object>> getUserActivityBreakdown(Location location, LocalDateTime[] dateRange) {
        Map<String, Object> breakdown = new LinkedHashMap<>();
        
        List<Booking> seatBookings = location != null ? 
            bookingRepository.findBySeatLocationAndTimeRange(location, dateRange[0], dateRange[1]) :
            bookingRepository.findByTimeRange(dateRange[0], dateRange[1]);
        
        List<RoomBooking> roomBookings = location != null ? 
            roomBookingRepository.findByRoomLocationAndTimeRange(location, dateRange[0], dateRange[1]) :
            roomBookingRepository.findByStartTimeBetween(dateRange[0], dateRange[1]);
        
        List<EquipmentRequest> equipmentRequests = location != null ? 
            equipmentRequestRepository.findByEquipmentLocationAndDateRange(location, dateRange[0], dateRange[1]) :
            equipmentRequestRepository.findByStartTimeBetween(dateRange[0], dateRange[1]);
        
        breakdown.put("totalSeatBookings", seatBookings.size());
        breakdown.put("totalRoomBookings", roomBookings.size());
        breakdown.put("totalEquipmentRequests", equipmentRequests.size());
        
        // Unique users across all activities
        List<Long> allActiveUserIds = new ArrayList<>();
        allActiveUserIds.addAll(seatBookings.stream().map(b -> b.getUser().getId()).collect(Collectors.toList()));
        allActiveUserIds.addAll(roomBookings.stream().map(b -> b.getUser().getId()).collect(Collectors.toList()));
        allActiveUserIds.addAll(equipmentRequests.stream().map(r -> r.getUser().getId()).collect(Collectors.toList()));
        
        breakdown.put("uniqueActiveUsers", allActiveUserIds.stream().distinct().count());
        breakdown.put("averageActivitiesPerUser", 
            allActiveUserIds.stream().distinct().count() > 0 ? 
                (double) allActiveUserIds.size() / allActiveUserIds.stream().distinct().count() : 0.0);
        
        return List.of(breakdown);
    }

    private List<Map<String, Object>> getTopActiveUsers(Location location, LocalDateTime[] dateRange, int limit) {
        Map<Long, Integer> userActivityCounts = new LinkedHashMap<>();
        
        // Count activities per user
        List<Booking> seatBookings = location != null ? 
            bookingRepository.findBySeatLocationAndTimeRange(location, dateRange[0], dateRange[1]) :
            bookingRepository.findByTimeRange(dateRange[0], dateRange[1]);
        
        for (Booking booking : seatBookings) {
            userActivityCounts.put(booking.getUser().getId(), 
                userActivityCounts.getOrDefault(booking.getUser().getId(), 0) + 1);
        }
        
        List<RoomBooking> roomBookings = location != null ? 
            roomBookingRepository.findByRoomLocationAndTimeRange(location, dateRange[0], dateRange[1]) :
            roomBookingRepository.findByStartTimeBetween(dateRange[0], dateRange[1]);
        
        for (RoomBooking booking : roomBookings) {
            userActivityCounts.put(booking.getUser().getId(), 
                userActivityCounts.getOrDefault(booking.getUser().getId(), 0) + 1);
        }
        
        List<EquipmentRequest> equipmentRequests = location != null ? 
            equipmentRequestRepository.findByEquipmentLocationAndDateRange(location, dateRange[0], dateRange[1]) :
            equipmentRequestRepository.findByStartTimeBetween(dateRange[0], dateRange[1]);
        
        for (EquipmentRequest request : equipmentRequests) {
            userActivityCounts.put(request.getUser().getId(), 
                userActivityCounts.getOrDefault(request.getUser().getId(), 0) + 1);
        }
        
        return userActivityCounts.entrySet().stream()
            .sorted(Map.Entry.<Long, Integer>comparingByValue().reversed())
            .limit(limit)
            .map(entry -> {
                User user = userRepository.findById(entry.getKey()).orElse(null);
                Map<String, Object> data = new LinkedHashMap<>();
                data.put("userName", user != null ? user.getFullName() : "Unknown");
                data.put("userEmail", user != null ? user.getEmail() : "Unknown");
                data.put("userType", user != null ? (user.isStudent() ? "Student" : "Staff") : "Unknown");
                data.put("activityCount", entry.getValue());
                return data;
            })
            .collect(Collectors.toList());
    }

    private List<Map<String, Object>> getProfessorApprovalStatus(Location location) {
        List<User> professors = location != null ? 
            userRepository.findProfessorsByLocation(location) :
            userRepository.findAllHods();
        
        Map<String, Long> approvalStatus = professors.stream()
            .collect(Collectors.groupingBy(
                professor -> professor.isProfessorApproved() ? "Approved" : "Pending",
                Collectors.counting()));
        
        Map<String, Object> statusData = new LinkedHashMap<>();
        statusData.put("totalProfessors", professors.size());
        statusData.put("approvedProfessors", approvalStatus.getOrDefault("Approved", 0L));
        statusData.put("pendingProfessors", approvalStatus.getOrDefault("Pending", 0L));
        
        return List.of(statusData);
    }

    private List<Map<String, Object>> getLibrarianWorkload(Location location, LocalDateTime[] dateRange) {
        List<User> librarians = location != null ? 
            userRepository.findLibrariansByLocation(location) :
            userRepository.findAllLibrarians();
        
        return librarians.stream()
            .map(librarian -> {
                Map<String, Object> data = new LinkedHashMap<>();
                data.put("librarianName", librarian.getFullName());
                data.put("location", librarian.getLocation().toString());
                data.put("activeThisWeek", librarian.isActiveThisWeek());
                data.put("isDefault", librarian.isDefaultLibrarian());
                data.put("workingDays", librarian.getWorkingDays() != null ? 
                    librarian.getWorkingDays().toString() : "Not Set");
                
                // Could add more workload metrics here
                return data;
            })
            .collect(Collectors.toList());
    }

        
    private List<Map<String, Object>> getNewUserTrends(Location location, LocalDateTime[] dateRange) {
        // Monthly new user registration trends
        Map<String, Long> monthlyNewUsers = new LinkedHashMap<>();
        
        LocalDateTime current = dateRange[0];
        while (current.isBefore(dateRange[1])) {
            LocalDateTime monthStart = current.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
            LocalDateTime monthEnd = monthStart.plusMonths(1).minusSeconds(1);
            
            long newUsersInMonth = location != null ? 
                userRepository.countByLocationAndCreatedAtBetween(location, monthStart, monthEnd) :
                userRepository.countByCreatedAtBetween(monthStart, monthEnd);
            
            String monthLabel = monthStart.format(DateTimeFormatter.ofPattern("MMM yyyy"));
            monthlyNewUsers.put(monthLabel, newUsersInMonth);
            
            current = current.plusMonths(1);
        }
        
        return monthlyNewUsers.entrySet().stream()
            .map(entry -> {
                Map<String, Object> data = new LinkedHashMap<>();
                data.put("month", entry.getKey());
                data.put("newUsers", entry.getValue());
                return data;
            })
            .collect(Collectors.toList());
    }

    // ===== PREVIOUS PERIOD COMPARISON METHODS =====

    private int getPreviousTotalUsers(Location location, LocalDateTime[] dateRange) {
        long periodDays = ChronoUnit.DAYS.between(dateRange[0], dateRange[1]);
        LocalDateTime cutoffDate = dateRange[0];
        
        if (location != null) {
            return userRepository.countByLocationAndCreatedAtBefore(location, cutoffDate);
        }
        return userRepository.countByCreatedAtBefore(cutoffDate);
    }

    private int getPreviousActiveToday(Location location, LocalDateTime[] dateRange) {
        long periodDays = ChronoUnit.DAYS.between(dateRange[0], dateRange[1]);
        LocalDateTime prevDay = LocalDateTime.now().minusDays(periodDays);
        LocalDateTime prevDayStart = prevDay.toLocalDate().atStartOfDay();
        LocalDateTime prevDayEnd = prevDayStart.plusDays(1).minusSeconds(1);
        
        // Calculate active users for previous equivalent day
        List<Long> activeUserIds = new ArrayList<>();
        
        List<Booking> prevBookings = location != null ? 
            bookingRepository.findBySeatLocationAndTimeRange(location, prevDayStart, prevDayEnd) :
            bookingRepository.findByTimeRange(prevDayStart, prevDayEnd);
        activeUserIds.addAll(prevBookings.stream()
            .map(booking -> booking.getUser().getId())
            .collect(Collectors.toList()));
        
        List<RoomBooking> prevRoomBookings = location != null ? 
            roomBookingRepository.findByRoomLocationAndTimeRange(location, prevDayStart, prevDayEnd) :
            roomBookingRepository.findByStartTimeBetween(prevDayStart, prevDayEnd);
        activeUserIds.addAll(prevRoomBookings.stream()
            .map(booking -> booking.getUser().getId())
            .collect(Collectors.toList()));
        
        return (int) activeUserIds.stream().distinct().count();
    }

    private int getPreviousNewMonth(Location location, LocalDateTime[] dateRange) {
        LocalDateTime prevMonthStart = LocalDateTime.now().minusMonths(2).withDayOfMonth(1)
            .withHour(0).withMinute(0).withSecond(0);
        LocalDateTime prevMonthEnd = LocalDateTime.now().minusMonths(1).withDayOfMonth(1)
            .withHour(0).withMinute(0).withSecond(0).minusSeconds(1);
        
        if (location != null) {
            return userRepository.countByLocationAndCreatedAtBetween(location, prevMonthStart, prevMonthEnd);
        }
        return userRepository.countByCreatedAtBetween(prevMonthStart, prevMonthEnd);
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