package com.auca.library.repository;

import com.auca.library.model.LibraryAnnouncement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface LibraryAnnouncementRepository extends JpaRepository<LibraryAnnouncement, Long> {
    
    @Query("SELECT a FROM LibraryAnnouncement a WHERE " +
           "a.startDateTime <= :now AND a.endDateTime > :now AND a.isUIVisible = true " +
           "ORDER BY a.createdAt DESC")
    List<LibraryAnnouncement> findActiveVisibleAnnouncements(@Param("now") LocalDateTime now);
    
    @Query("SELECT a FROM LibraryAnnouncement a WHERE " +
           "a.startDateTime <= :now AND a.endDateTime > :now AND a.isNotificationEnabled = true " +
           "ORDER BY a.createdAt DESC")
    List<LibraryAnnouncement> findActiveNotifiableAnnouncements(@Param("now") LocalDateTime now);
    
    @Query("SELECT a FROM LibraryAnnouncement a WHERE " +
           "a.createdBy.id = :userId " +
           "ORDER BY a.createdAt DESC")
    List<LibraryAnnouncement> findByCreatedById(@Param("userId") Long userId);
    
    @Query("SELECT a FROM LibraryAnnouncement a WHERE " +
           "a.endDateTime < :cutoff")
    List<LibraryAnnouncement> findExpiredAnnouncements(@Param("cutoff") LocalDateTime cutoff);
}