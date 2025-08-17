package com.auca.library.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.auca.library.model.Equipment;
import com.auca.library.model.EquipmentLog;
import com.auca.library.model.Location;
import com.auca.library.model.User;

@Repository
public interface EquipmentLogRepository extends JpaRepository<EquipmentLog, Long> {
    
    List<EquipmentLog> findByEquipmentOrderByChangedAtDesc(Equipment equipment);
    
    List<EquipmentLog> findByChangedByOrderByChangedAtDesc(User user);
    
    @Query("SELECT el FROM EquipmentLog el WHERE el.equipment.location = :location ORDER BY el.changedAt DESC")
    List<EquipmentLog> findByLocationOrderByChangedAtDesc(@Param("location") Location location);
    
    @Query("SELECT el FROM EquipmentLog el WHERE el.changedAt BETWEEN :startDate AND :endDate ORDER BY el.changedAt DESC")
    List<EquipmentLog> findByDateRangeOrderByChangedAtDesc(@Param("startDate") LocalDateTime startDate, 
                                                          @Param("endDate") LocalDateTime endDate);
}