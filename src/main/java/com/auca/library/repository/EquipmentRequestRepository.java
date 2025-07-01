package com.auca.library.repository;

import com.auca.library.model.EquipmentRequest;
import com.auca.library.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface EquipmentRequestRepository extends JpaRepository<EquipmentRequest, Long> {
    
    List<EquipmentRequest> findByUserOrderByCreatedAtDesc(User user);
    
    List<EquipmentRequest> findByStatus(EquipmentRequest.RequestStatus status);
    
    @Query("SELECT er FROM EquipmentRequest er WHERE er.status = 'PENDING' ORDER BY er.createdAt ASC")
    List<EquipmentRequest> findPendingRequests();
    
    @Query("SELECT er FROM EquipmentRequest er WHERE er.status = 'ESCALATED' ORDER BY er.escalatedAt ASC")
    List<EquipmentRequest> findEscalatedRequests();
    
    List<EquipmentRequest> findByUserAndStatus(User user, EquipmentRequest.RequestStatus status);
    
    @Query("SELECT er FROM EquipmentRequest er " +
           "WHERE er.equipment.id = :equipmentId " +
           "AND er.status IN ('APPROVED', 'HOD_APPROVED') " +
           "AND ((er.startTime <= :endTime AND er.endTime >= :startTime))")
    List<EquipmentRequest> findConflictingRequests(@Param("equipmentId") Long equipmentId,
                                                  @Param("startTime") LocalDateTime startTime,
                                                  @Param("endTime") LocalDateTime endTime);
    
    @Query("SELECT er FROM EquipmentRequest er WHERE er.roomBooking.id = :bookingId")
    List<EquipmentRequest> findByRoomBookingId(@Param("bookingId") Long bookingId);
    
    @Query("SELECT er FROM EquipmentRequest er WHERE er.labClass.id = :labId")
    List<EquipmentRequest> findByLabClassId(@Param("labId") Long labId);
}
