package com.auca.library.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.auca.library.model.Equipment;
import com.auca.library.model.EquipmentInventory;
import com.auca.library.model.EquipmentStatus;
import com.auca.library.model.Location;

@Repository
public interface EquipmentInventoryRepository extends JpaRepository<EquipmentInventory, Long> {
    
    List<EquipmentInventory> findByEquipment(Equipment equipment);
    
    Optional<EquipmentInventory> findByEquipmentAndStatus(Equipment equipment, EquipmentStatus status);
    
    @Query("SELECT ei FROM EquipmentInventory ei WHERE ei.equipment.id = :equipmentId")
    List<EquipmentInventory> findByEquipmentId(@Param("equipmentId") Long equipmentId);
    
    @Query("SELECT SUM(ei.quantity) FROM EquipmentInventory ei WHERE ei.equipment = :equipment")
    Integer getTotalQuantityForEquipment(@Param("equipment") Equipment equipment);
    
    @Query("SELECT ei.quantity FROM EquipmentInventory ei WHERE ei.equipment = :equipment AND ei.status = 'AVAILABLE'")
    Integer getAvailableQuantityForEquipment(@Param("equipment") Equipment equipment);
    
    @Query("SELECT ei FROM EquipmentInventory ei WHERE ei.equipment.location = :location")
    List<EquipmentInventory> findByLocation(@Param("location") Location location);
}