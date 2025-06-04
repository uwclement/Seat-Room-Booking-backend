package com.auca.library.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.auca.library.model.RecurringBookingSeries;
import com.auca.library.model.Room;
import com.auca.library.model.User;

@Repository
public interface RecurringBookingSeriesRepository extends JpaRepository<RecurringBookingSeries, Long> {
    
    List<RecurringBookingSeries> findByUserAndIsActiveTrue(User user);
    
    List<RecurringBookingSeries> findByRoomAndIsActiveTrue(Room room);
    
    @Query("SELECT rbs FROM RecurringBookingSeries rbs WHERE rbs.isActive = true AND " +
           "(rbs.lastGeneratedDate IS NULL OR rbs.lastGeneratedDate < :cutoffDate)")
    List<RecurringBookingSeries> findSeriesNeedingGeneration(@Param("cutoffDate") LocalDateTime cutoffDate);
    
    @Query("SELECT COUNT(rbs) FROM RecurringBookingSeries rbs WHERE rbs.user = :user AND " +
           "rbs.isActive = true")
    Long countActiveSeriesForUser(@Param("user") User user);
}