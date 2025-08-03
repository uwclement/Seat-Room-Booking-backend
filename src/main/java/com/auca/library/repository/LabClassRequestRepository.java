package com.auca.library.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.auca.library.model.LabRequest;
import com.auca.library.model.User;

@Repository
public interface LabClassRequestRepository extends JpaRepository<LabRequest, Long> {
    
    List<LabRequest> findByUserOrderByCreatedAtDesc(User user);
    
    List<LabRequest> findByStatus(LabRequest.RequestStatus status);
    
    @Query("SELECT lr FROM LabRequest lr WHERE lr.status = 'PENDING' ORDER BY lr.createdAt ASC")
    List<LabRequest> findPendingRequests();
    
    @Query("SELECT lr FROM LabRequest lr WHERE lr.status = 'ESCALATED' ORDER BY lr.escalatedAt ASC")
    List<LabRequest> findEscalatedRequests();
    
    @Query("SELECT lr FROM LabRequest lr " +
           "WHERE lr.labClass.id = :labId " +
           "AND lr.status IN ('APPROVED', 'HOD_APPROVED') " +
           "AND ((lr.startTime <= :endTime AND lr.endTime >= :startTime))")
    List<LabRequest> findConflictingLabRequests(@Param("labId") Long labId,
                                                    @Param("startTime") LocalDateTime startTime,
                                                    @Param("endTime") LocalDateTime endTime);


       @Query("SELECT lr FROM LabRequest lr WHERE lr.createdAt >= :startDate AND lr.createdAt <= :endDate ORDER BY lr.createdAt DESC")
     List<LabRequest> findRequestsInDateRange(@Param("startDate") LocalDateTime startDate, 
                                             @Param("endDate") LocalDateTime endDate);                                             
}