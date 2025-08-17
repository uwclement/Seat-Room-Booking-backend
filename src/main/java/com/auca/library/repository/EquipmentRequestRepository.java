package com.auca.library.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.auca.library.model.EquipmentRequest;
import com.auca.library.model.User;
import com.auca.library.model.Location;
import com.auca.library.model.RoomBooking;

@Repository
public interface EquipmentRequestRepository extends JpaRepository<EquipmentRequest, Long> {
    
    List<EquipmentRequest> findByUserOrderByCreatedAtDesc(User user);
    
    List<EquipmentRequest> findByStatus(EquipmentRequest.RequestStatus status);
    
    @Query("SELECT er FROM EquipmentRequest er WHERE er.status = com.auca.library.model.EquipmentRequest.RequestStatus.PENDING ORDER BY er.createdAt ASC")
    List<EquipmentRequest> findPendingRequests();
    
    @Query("SELECT er FROM EquipmentRequest er WHERE er.status = 'ESCALATED' ORDER BY er.escalatedAt ASC")
    List<EquipmentRequest> findEscalatedRequests();
    
    List<EquipmentRequest> findByUserAndStatus(User user, EquipmentRequest.RequestStatus status);
    
//     @Query("SELECT er FROM EquipmentRequest er " +
//            "WHERE er.equipment.id = :equipmentId " +
//            "AND er.status IN ('APPROVED', 'HOD_APPROVED') " +
//            "AND ((er.startTime <= :endTime AND er.endTime >= :startTime))")
//     List<EquipmentRequest> findConflictingRequests(@Param("equipmentId") Long equipmentId,
//                                                   @Param("startTime") LocalDateTime startTime,
//                                                   @Param("endTime") LocalDateTime endTime);
    
    @Query("SELECT er FROM EquipmentRequest er WHERE er.roomBooking.id = :bookingId")
    List<EquipmentRequest> findByRoomBookingId(@Param("bookingId") Long bookingId);
    
    @Query("SELECT er FROM EquipmentRequest er WHERE er.labClass.id = :labId")
    List<EquipmentRequest> findByLabClassId(@Param("labId") Long labId);

    @Query("SELECT er FROM EquipmentRequest er WHERE er.createdAt >= :startDate AND er.createdAt <= :endDate ORDER BY er.createdAt DESC")
    List<EquipmentRequest> findRequestsInDateRange(@Param("startDate") LocalDateTime startDate, 
                                             @Param("endDate") LocalDateTime endDate);

     
                                             
   @Query("SELECT er FROM EquipmentRequest er WHERE er.escalatedToHod = true AND er.createdAt >= :startDate AND er.createdAt <= :endDate ORDER BY er.createdAt DESC")
   List<EquipmentRequest> findHodRequestsInDateRange(@Param("startDate") LocalDateTime startDate,
                                                  @Param("endDate") LocalDateTime endDate);


    // Add these methods to EquipmentRequestRepository

@Query("SELECT er FROM EquipmentRequest er WHERE er.status IN ('APPROVED', 'IN_USE', 'HOD_APPROVED') ORDER BY er.startTime ASC")
List<EquipmentRequest> findActiveRequests();

@Query("SELECT er FROM EquipmentRequest er WHERE er.extensionStatus = 'PENDING' ORDER BY er.extensionRequestedAt ASC")
List<EquipmentRequest> findPendingExtensionRequests();

@Query("SELECT COALESCE(SUM(er.extensionHoursRequested), 0) FROM EquipmentRequest er WHERE er.user.id = :userId AND er.extensionRequestedAt BETWEEN :startOfDay AND :endOfDay AND er.extensionStatus = 'APPROVED'")
Double getTotalExtensionHoursForUserToday(@Param("userId") Long userId, @Param("startOfDay") LocalDateTime startOfDay, @Param("endOfDay") LocalDateTime endOfDay);

@Query("SELECT er FROM EquipmentRequest er WHERE er.status = :status AND er.returnedAt IS NOT NULL")
List<EquipmentRequest> findByStatusAndReturnedAtIsNotNull(@Param("status") EquipmentRequest.RequestStatus status);

// Update existing method to include extension fields
@Query("SELECT er FROM EquipmentRequest er WHERE er.equipment.id = :equipmentId " +
       "AND ((er.startTime <= :endTime AND er.endTime >= :startTime) " +
       "OR (er.extensionStatus = 'APPROVED' AND er.startTime <= :endTime AND er.endTime >= :startTime)) " +
       "AND er.status IN ('PENDING', 'APPROVED', 'IN_USE', 'HOD_APPROVED')")
List<EquipmentRequest> findConflictingRequests(@Param("equipmentId") Long equipmentId, 
                                               @Param("startTime") LocalDateTime startTime, 
                                               @Param("endTime") LocalDateTime endTime); 
                                               
                                               
@Query("SELECT COUNT(er) FROM EquipmentRequest er WHERE er.equipment.location = :location AND " +
           "er.startTime >= :startTime AND er.endTime <= :endTime AND " +
           "(er.status = 'PENDING' OR er.status = 'APPROVED' OR er.status = 'IN_USE')")
    int countActiveByLocationAndDateRange(@Param("location") Location location,
                                         @Param("startTime") LocalDateTime startTime,
                                         @Param("endTime") LocalDateTime endTime);
    
    @Query("SELECT COUNT(er) FROM EquipmentRequest er WHERE " +
           "er.startTime >= :startTime AND er.endTime <= :endTime AND " +
           "(er.status = 'PENDING' OR er.status = 'APPROVED' OR er.status = 'IN_USE')")
    int countActiveByDateRange(@Param("startTime") LocalDateTime startTime,
                              @Param("endTime") LocalDateTime endTime);
    
    @Query("SELECT er FROM EquipmentRequest er WHERE er.equipment.location = :location AND " +
           "er.startTime >= :startTime AND er.endTime <= :endTime")
    List<EquipmentRequest> findByEquipmentLocationAndDateRange(@Param("location") Location location,
                                                              @Param("startTime") LocalDateTime startTime,
                                                              @Param("endTime") LocalDateTime endTime);
    
    @Query("SELECT er FROM EquipmentRequest er WHERE er.user = :user AND " +
           "er.startTime >= :startTime AND er.endTime <= :endTime")
    List<EquipmentRequest> findByUserAndTimeRange(@Param("user") User user,
                                                 @Param("startTime") LocalDateTime startTime,
                                                 @Param("endTime") LocalDateTime endTime);   
    
    List<EquipmentRequest> findByStartTimeBetween(LocalDateTime start, LocalDateTime end);                                            
                                     
}
