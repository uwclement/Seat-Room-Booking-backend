package com.auca.library.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.auca.library.model.BookingAnalytics;

@Repository
public interface BookingAnalyticsRepository extends JpaRepository<BookingAnalytics, Long> {
    
    // Room usage analytics
    @Query("SELECT ba.room, AVG(ba.bookingDurationMinutes), AVG(ba.actualUsageDurationMinutes), " +
           "COUNT(ba), SUM(CASE WHEN ba.wasNoShow THEN 1 ELSE 0 END) " +
           "FROM BookingAnalytics ba WHERE ba.date >= :startDate AND ba.date <= :endDate " +
           "GROUP BY ba.room ORDER BY COUNT(ba) DESC")
    List<Object[]> findRoomUsageStatistics(@Param("startDate") LocalDate startDate,
                                          @Param("endDate") LocalDate endDate);
    
    // User behavior analytics
    @Query("SELECT ba.user, COUNT(ba), AVG(ba.actualUsageDurationMinutes), " +
           "SUM(CASE WHEN ba.wasNoShow THEN 1 ELSE 0 END) as noShowCount " +
           "FROM BookingAnalytics ba WHERE ba.date >= :startDate AND ba.date <= :endDate " +
           "GROUP BY ba.user")
    List<Object[]> findUserBehaviorStatistics(@Param("startDate") LocalDate startDate,
                                             @Param("endDate") LocalDate endDate);
    
    // Peak time analysis
    @Query("SELECT HOUR(ba.createdAt), COUNT(ba) FROM BookingAnalytics ba " +
           "WHERE ba.date >= :startDate AND ba.date <= :endDate " +
           "GROUP BY HOUR(ba.createdAt) ORDER BY HOUR(ba.createdAt)")
    List<Object[]> findPeakBookingHours(@Param("startDate") LocalDate startDate,
                                       @Param("endDate") LocalDate endDate);
    
    // Daily statistics
    @Query("SELECT ba.date, COUNT(ba), AVG(ba.participantCount), " +
           "SUM(CASE WHEN ba.checkedIn THEN 1 ELSE 0 END) as checkedInCount " +
           "FROM BookingAnalytics ba WHERE ba.date >= :startDate AND ba.date <= :endDate " +
           "GROUP BY ba.date ORDER BY ba.date")
    List<Object[]> findDailyStatistics(@Param("startDate") LocalDate startDate,
                                      @Param("endDate") LocalDate endDate);
}