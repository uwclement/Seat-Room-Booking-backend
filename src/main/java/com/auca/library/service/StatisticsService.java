package com.auca.library.service;

// import com.auca.library.dto.response.StatisticsResponse;
// import com.auca.library.model.Booking;
// import com.auca.library.model.Booking.BookingStatus;
// import com.auca.library.model.Seat;
// import com.auca.library.repository.BookingRepository;
// import com.auca.library.repository.SeatRepository;
// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.stereotype.Service;

// import java.time.Duration;
// import java.time.LocalDate;
// import java.time.LocalDateTime;
// import java.time.LocalTime;
// import java.time.temporal.ChronoUnit;
// import java.util.*;
// import java.util.stream.Collectors;

// @Service
public class StatisticsService {

    // @Autowired
    // private BookingRepository bookingRepository;
    
    // @Autowired
    // private SeatRepository seatRepository;

    // public StatisticsResponse getDailyStatistics(LocalDate date) {
    //     LocalDateTime startOfDay = date.atStartOfDay();
    //     LocalDateTime endOfDay = date.atTime(LocalTime.MAX);
        
    //     List<Booking> bookings = bookingRepository.findByStartTimeBetween(startOfDay, endOfDay);
        
    //     StatisticsResponse statistics = new StatisticsResponse();
    //     statistics.setTotalBookings(bookings.size());
        
    //     Set<Long> uniqueUsers = bookings.stream()
    //             .map(booking -> booking.getUser().getId())
    //             .collect(Collectors.toSet());
    //     statistics.setTotalUniqueUsers(uniqueUsers.size());
        
    //     // Calculate average booking duration
    //     double totalDurationMinutes = bookings.stream()
    //             .filter(booking -> booking.getCheckinTime() != null && booking.getCheckoutTime() != null)
    //             .mapToLong(booking -> ChronoUnit.MINUTES.between(booking.getCheckinTime(), booking.getCheckoutTime()))
    //             .average()
    //             .orElse(0);
    //     statistics.setAverageBookingDuration(totalDurationMinutes);
        
    //     // Calculate peak occupancy
    //     Map<Integer, Integer> bookingsByHour = new HashMap<>();
    //     for (int hour = 0; hour < 24; hour++) {
    //         LocalDateTime hourStart = date.atTime(hour, 0);
    //         LocalDateTime hourEnd = date.atTime(hour, 59, 59);
            
    //         int hourlyBookings = (int) bookings.stream()
    //                 .filter(booking -> 
    //                         (booking.getStartTime().isBefore(hourEnd) || booking.getStartTime().isEqual(hourEnd)) &&
    //                         (booking.getEndTime().isAfter(hourStart) || booking.getEndTime().isEqual(hourStart)))
    //                 .count();
            
    //         bookingsByHour.put(hour, hourlyBookings);
    //     }
        
    //     // Total seats count
    //     int totalSeats = seatRepository.findAll().size();
        
    //     // Peak occupancy rate
    //     double peakOccupancy = bookingsByHour.values().stream()
    //             .mapToInt(Integer::intValue)
    //             .max()
    //             .orElse(0) / (double) totalSeats * 100;
    //     statistics.setPeakOccupancyRate(peakOccupancy);
        
    //     // Average occupancy rate
    //     double avgOccupancy = bookingsByHour.values().stream()
    //             .mapToInt(Integer::intValue)
    //             .average()
    //             .orElse(0) / totalSeats * 100;
    //     statistics.setAverageOccupancyRate(avgOccupancy);
        
    //     // Bookings by zone
    //     Map<String, Integer> bookingsByZone = bookings.stream()
    //             .collect(Collectors.groupingBy(
    //                     booking -> booking.getSeat().getZoneType(),
    //                     Collectors.summingInt(booking -> 1)
    //             ));
    //     statistics.setBookingsByZone(bookingsByZone);
        
    //     // Bookings by hour
    //     Map<String, Integer> formattedBookingsByHour = new HashMap<>();
    //     bookingsByHour.forEach((hour, count) -> {
    //         formattedBookingsByHour.put(String.format("%02d:00", hour), count);
    //     });
    //     statistics.setBookingsByHour(formattedBookingsByHour);
        
    //     return statistics;
    // }
    
    // public Map<String, StatisticsResponse> getWeeklyStatistics(LocalDate startDate) {
    //     Map<String, StatisticsResponse> weeklyStats = new HashMap<>();
        
    //     for (int i = 0; i < 7; i++) {
    //         LocalDate currentDate = startDate.plusDays(i);
    //         StatisticsResponse dailyStats = getDailyStatistics(currentDate);
    //         weeklyStats.put(currentDate.toString(), dailyStats);
    //     }
        
    //     return weeklyStats;
    // }
    
    // public Map<String, StatisticsResponse> getMonthlyStatistics(Integer year, Integer month) {
    //     Map<String, StatisticsResponse> monthlyStats = new HashMap<>();
        
    //     LocalDate firstDayOfMonth = LocalDate.of(year, month, 1);
    //     LocalDate lastDayOfMonth = firstDayOfMonth.plusMonths(1).minusDays(1);
        
    //     for (int i = 1; i <= lastDayOfMonth.getDayOfMonth(); i++) {
    //         LocalDate currentDate = LocalDate.of(year, month, i);
    //         StatisticsResponse dailyStats = getDailyStatistics(currentDate);
    //         monthlyStats.put(currentDate.toString(), dailyStats);
    //     }
        
    //     return monthlyStats;
    // }
    
    // public Map<Integer, Integer> getPeakHours(LocalDate date) {
    //     LocalDateTime startOfDay = date.atStartOfDay();
    //     LocalDateTime endOfDay = date.atTime(LocalTime.MAX);
        
    //     List<Booking> bookings = bookingRepository.findByStartTimeBetween(startOfDay, endOfDay);
        
    //     Map<Integer, Integer> bookingsByHour = new HashMap<>();
    //     for (int hour = 0; hour < 24; hour++) {
    //         LocalDateTime hourStart = date.atTime(hour, 0);
    //         LocalDateTime hourEnd = date.atTime(hour, 59, 59);
            
    //         int hourlyBookings = (int) bookings.stream()
    //                 .filter(booking -> 
    //                         (booking.getStartTime().isBefore(hourEnd) || booking.getStartTime().isEqual(hourEnd)) &&
    //                         (booking.getEndTime().isAfter(hourStart) || booking.getEndTime().isEqual(hourStart)))
    //                 .count();
            
    //         bookingsByHour.put(hour, hourlyBookings);
    //     }
        
    //     return bookingsByHour;
    // }
    
    // public Map<String, Double> getZoneUtilization(LocalDate date) {
    //     LocalDateTime startOfDay = date.atStartOfDay();
    //     LocalDateTime endOfDay = date.atTime(LocalTime.MAX);
        
    //     List<Booking> bookings = bookingRepository.findByStartTimeBetween(startOfDay, endOfDay);
    //     List<Seat> allSeats = seatRepository.findAll();
        
    //     // Group seats by zone type
    //     Map<String, Long> seatsByZone = allSeats.stream()
    //             .collect(Collectors.groupingBy(Seat::getZoneType, Collectors.counting()));
        
    //     // Group bookings by zone type
    //     Map<String, Long> bookingsByZone = bookings.stream()
    //             .collect(Collectors.groupingBy(
    //                     booking -> booking.getSeat().getZoneType(),
    //                     Collectors.counting()
    //             ));
        
    //     // Calculate utilization percentage for each zone
    //     Map<String, Double> zoneUtilization = new HashMap<>();
        
    //     seatsByZone.forEach((zone, seatCount) -> {
    //         long bookingCount = bookingsByZone.getOrDefault(zone, 0L);
    //         double utilizationRate = (double) bookingCount / seatCount * 100;
    //         zoneUtilization.put(zone, utilizationRate);
    //     });
        
    //     return zoneUtilization;
    // }
    
    // public Map<Long, Double> getSeatUtilization(LocalDate date) {
    //     LocalDateTime startOfDay = date.atStartOfDay();
    //     LocalDateTime endOfDay = date.atTime(LocalTime.MAX);
        
    //     List<Booking> bookings = bookingRepository.findByStartTimeBetween(startOfDay, endOfDay);
        
    //     // Calculate total available minutes in a day (24 hours * 60 minutes)
    //     long totalMinutesInDay = 24 * 60;
        
    //     // Group bookings by seat and calculate total booked minutes
    //     Map<Long, Double> seatUtilization = new HashMap<>();
        
    //     bookings.forEach(booking -> {
    //         Long seatId = booking.getSeat().getId();
            
    //         // Calculate booking duration (capped to the day boundaries)
    //         LocalDateTime bookingStart = booking.getStartTime().isBefore(startOfDay) ? 
    //                 startOfDay : booking.getStartTime();
    //         LocalDateTime bookingEnd = booking.getEndTime().isAfter(endOfDay) ? 
    //                 endOfDay : booking.getEndTime();
            
    //         long bookingMinutes = ChronoUnit.MINUTES.between(bookingStart, bookingEnd);
            
    //         // Add to existing total or create new entry
    //         seatUtilization.merge(seatId, 
    //                 (double) bookingMinutes / totalMinutesInDay * 100, 
    //                 Double::sum);
    //     });
        
    //     // Ensure all seats are represented (even those with no bookings)
    //     seatRepository.findAll().forEach(seat -> {
    //         seatUtilization.putIfAbsent(seat.getId(), 0.0);
    //     });
        
    //     return seatUtilization;
    // }
    
    // public Map<String, Double> getAverageBookingDuration(LocalDate startDate, LocalDate endDate) {
    //     LocalDateTime startDateTime = startDate.atStartOfDay();
    //     LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);
        
    //     List<Booking> bookings = bookingRepository.findByStartTimeBetween(startDateTime, endDateTime);
        
    //     // Overall average
    //     double overallAverage = bookings.stream()
    //             .filter(booking -> booking.getCheckinTime() != null && booking.getCheckoutTime() != null)
    //             .mapToLong(booking -> ChronoUnit.MINUTES.between(booking.getCheckinTime(), booking.getCheckoutTime()))
    //             .average()
    //             .orElse(0);
        
    //     // Average by zone
    //     Map<String, Double> averageByZone = bookings.stream()
    //             .filter(booking -> booking.getCheckinTime() != null && booking.getCheckoutTime() != null)
    //             .collect(Collectors.groupingBy(
    //                     booking -> booking.getSeat().getZoneType(),
    //                     Collectors.averagingLong(booking -> 
    //                             ChronoUnit.MINUTES.between(booking.getCheckinTime(), booking.getCheckoutTime()))
    //             ));
        
    //     // Add overall average to the results
    //     Map<String, Double> result = new HashMap<>(averageByZone);
    //     result.put("overall", overallAverage);
        
    //     return result;
    // }
}