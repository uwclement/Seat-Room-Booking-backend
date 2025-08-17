package com.auca.library.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.auca.library.model.EquipmentAssignment;
import com.auca.library.model.EquipmentUnit;
import com.auca.library.model.Location;
import com.auca.library.model.User;

@Repository
public interface EquipmentAssignmentRepository extends JpaRepository<EquipmentAssignment, Long> {
    
    List<EquipmentAssignment> findByEquipmentUnit(EquipmentUnit equipmentUnit);
    
    List<EquipmentAssignment> findByAssignedToUser(User user);
    
    List<EquipmentAssignment> findByStatus(EquipmentAssignment.AssignmentStatus status);
    
    @Query("SELECT ea FROM EquipmentAssignment ea WHERE ea.equipmentUnit.equipment.location = :location")
    List<EquipmentAssignment> findByLocation(@Param("location") Location location);
    
    @Query("SELECT ea FROM EquipmentAssignment ea WHERE ea.equipmentUnit.equipment.location = :location AND ea.status = :status")
    List<EquipmentAssignment> findByLocationAndStatus(@Param("location") Location location, @Param("status") EquipmentAssignment.AssignmentStatus status);
    
    @Query("SELECT ea FROM EquipmentAssignment ea WHERE ea.equipmentUnit = :equipmentUnit AND ea.status = 'ACTIVE'")
    Optional<EquipmentAssignment> findActiveByEquipmentUnit(@Param("equipmentUnit") EquipmentUnit equipmentUnit);
    
    @Query("SELECT ea FROM EquipmentAssignment ea WHERE ea.assignedToUser = :user AND ea.status = 'ACTIVE'")
    List<EquipmentAssignment> findActiveByUser(@Param("user") User user);
    
    @Query("SELECT COUNT(ea) FROM EquipmentAssignment ea WHERE ea.equipmentUnit.equipment.location = :location AND ea.status = 'ACTIVE'")
    Long countActiveByLocation(@Param("location") Location location);
    
    @Query("SELECT COUNT(ea) FROM EquipmentAssignment ea WHERE ea.equipmentUnit.equipment.location = :location AND ea.assignmentType = 'STAFF_ASSIGNMENT' AND ea.status = 'ACTIVE'")
    Long countActiveStaffAssignmentsByLocation(@Param("location") Location location);
    
    @Query("SELECT COUNT(ea) FROM EquipmentAssignment ea WHERE ea.equipmentUnit.equipment.location = :location AND ea.assignmentType = 'ROOM_ASSIGNMENT' AND ea.status = 'ACTIVE'")
    Long countActiveRoomAssignmentsByLocation(@Param("location") Location location);
    
    // Find expired temporary assignments
    @Query("SELECT ea FROM EquipmentAssignment ea WHERE ea.assignmentPeriod = 'TEMPORARY' AND ea.endDate < :currentTime AND ea.status = 'ACTIVE'")
    List<EquipmentAssignment> findExpiredTemporaryAssignments(@Param("currentTime") LocalDateTime currentTime);


    @Query("SELECT ea FROM EquipmentAssignment ea WHERE ea.equipmentUnit.equipment.location = :location AND " +
           "ea.startDate >= :startTime AND ea.startDate <= :endTime")
    List<EquipmentAssignment> findByLocationAndDateRange(@Param("location") Location location,
                                                        @Param("startTime") LocalDateTime startTime,
                                                        @Param("endTime") LocalDateTime endTime);
    
    @Query("SELECT ea FROM EquipmentAssignment ea WHERE " +
           "ea.startDate >= :startTime AND ea.startDate <= :endTime")
    List<EquipmentAssignment> findByStartDateBetween(@Param("startTime") LocalDateTime startTime,
                                                    @Param("endTime") LocalDateTime endTime);
}