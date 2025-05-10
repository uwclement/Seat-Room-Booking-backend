package com.auca.library.repository;

 import com.auca.library.model.BookingStatistics;
 import org.springframework.data.jpa.repository.JpaRepository;
 import org.springframework.stereotype.Repository;

// import java.time.DayOfWeek;
// import java.time.LocalTime;
// import java.util.List;
// import java.util.Optional;

@Repository
public interface BookingStatisticsRepository extends JpaRepository<BookingStatistics, Long> {
    // Optional<BookingStatistics> findByDayOfWeekAndHourOfDay(DayOfWeek dayOfWeek, LocalTime hourOfDay);
    
    // List<BookingStatistics> findByDayOfWeekOrderByHourOfDay(DayOfWeek dayOfWeek);
    
    // List<BookingStatistics> findAllByOrderByOccupancyRateDesc();
}