package com.auca.library.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.auca.library.model.Room;
import com.auca.library.model.RoomWaitlist;
import com.auca.library.model.User;

@Repository
public interface RoomWaitlistRepository extends JpaRepository<RoomWaitlist, Long> {
    
    List<RoomWaitlist> findByUserAndIsActiveTrueOrderByCreatedAtDesc(User user);
    
    @Query("SELECT rw FROM RoomWaitlist rw WHERE rw.room = :room AND rw.isActive = true AND " +
           "rw.desiredStartTime <= :endTime AND rw.desiredEndTime >= :startTime " +
           "ORDER BY rw.priority DESC, rw.createdAt ASC")
    List<RoomWaitlist> findWaitlistForTimeSlot(@Param("room") Room room,
                                              @Param("startTime") LocalDateTime startTime,
                                              @Param("endTime") LocalDateTime endTime);
    
    @Query("SELECT rw FROM RoomWaitlist rw WHERE rw.isActive = true AND " +
           "(rw.expiresAt IS NULL OR rw.expiresAt > :now)")
    List<RoomWaitlist> findActiveWaitlistEntries(@Param("now") LocalDateTime now);
    
    @Query("SELECT rw FROM RoomWaitlist rw WHERE rw.expiresAt <= :now")
    List<RoomWaitlist> findExpiredWaitlistEntries(@Param("now") LocalDateTime now);
}