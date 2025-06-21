package com.auca.library.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.auca.library.model.QRCodeLog;
import com.auca.library.model.User;

import jakarta.transaction.Transactional;

@Repository
public interface QRCodeLogRepository extends JpaRepository<QRCodeLog, Long> {
    
    List<QRCodeLog> findByResourceTypeAndResourceId(String resourceType, Long resourceId);
    
    List<QRCodeLog> findByGeneratedBy(User generatedBy);
    
    List<QRCodeLog> findByGeneratedAtBetween(LocalDateTime startDate, LocalDateTime endDate);
    
    @Query("SELECT q FROM QRCodeLog q WHERE q.resourceType = :type AND q.resourceId = :id AND q.isCurrent = true")
    QRCodeLog findCurrentQRCode(@Param("type") String resourceType, @Param("id") Long resourceId);
    
    @Query("SELECT COUNT(q) FROM QRCodeLog q WHERE q.generatedAt >= :since")
    Long countGeneratedSince(@Param("since") LocalDateTime since);
    
    @Query("SELECT q FROM QRCodeLog q WHERE q.resourceType = :type ORDER BY q.generatedAt DESC")
    List<QRCodeLog> findRecentByType(@Param("type") String resourceType);

    @Modifying
    @Transactional
    @Query("UPDATE QRCodeLog q SET q.isCurrent = false WHERE q.resourceType = :resourceType AND q.resourceId = :resourceId")
    void markAllAsNotCurrentForResource(@Param("resourceType") String resourceType, @Param("resourceId") Long resourceId);
}