package com.auca.library.service;

// import com.auca.library.dto.response.BookingStatisticsDTO;
// import com.auca.library.model.BookingStatistics;
// import com.auca.library.repository.BookingStatisticsRepository;
// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.stereotype.Service;

// import java.time.DayOfWeek;
// import java.time.LocalDateTime;
// import java.util.List;
// import java.util.stream.Collectors;

// @Service
public class BookingStatisticsService {

//    @Autowired
//    private BookingStatisticsRepository bookingStatisticsRepository;
   
//    // Get peak times
//    public List<BookingStatisticsDTO> getPeakTimes() {
//        List<BookingStatistics> stats = bookingStatisticsRepository.findAllByOrderByOccupancyRateDesc();
       
//        double peakThreshold = 0.7; // 70% occupancy is considered peak
       
//        return stats.stream()
//                .map(stat -> {
//                    BookingStatisticsDTO dto = new BookingStatisticsDTO();
//                    dto.setDayOfWeek(stat.getDayOfWeek());
//                    dto.setHourOfDay(stat.getHourOfDay());
//                    dto.setBookingCount(stat.getBookingCount());
//                    dto.setOccupancyRate(stat.getOccupancyRate());
//                    dto.setPeakTime(stat.getOccupancyRate() >= peakThreshold);
//                    return dto;
//                })
//                .collect(Collectors.toList());
//    }
   
//    // Get current day statistics
//    public List<BookingStatisticsDTO> getCurrentDayStatistics() {
//        DayOfWeek today = LocalDateTime.now().getDayOfWeek();
       
//        List<BookingStatistics> stats = bookingStatisticsRepository.findByDayOfWeekOrderByHourOfDay(today);
       
//        double peakThreshold = 0.7; // 70% occupancy is considered peak
       
//        return stats.stream()
//                .map(stat -> {
//                    BookingStatisticsDTO dto = new BookingStatisticsDTO();
//                    dto.setDayOfWeek(stat.getDayOfWeek());
//                    dto.setHourOfDay(stat.getHourOfDay());
//                    dto.setBookingCount(stat.getBookingCount());
//                    dto.setOccupancyRate(stat.getOccupancyRate());
//                    dto.setPeakTime(stat.getOccupancyRate() >= peakThreshold);
//                    return dto;
//                })
//                .collect(Collectors.toList());
//    }
}