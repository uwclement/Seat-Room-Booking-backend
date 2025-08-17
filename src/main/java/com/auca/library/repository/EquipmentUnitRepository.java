package com.auca.library.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.auca.library.model.Equipment;
import com.auca.library.model.EquipmentUnit;
import com.auca.library.model.Location;

@Repository
public interface EquipmentUnitRepository extends JpaRepository<EquipmentUnit, Long> {
    
    List<EquipmentUnit> findByEquipment(Equipment equipment);
    
    List<EquipmentUnit> findByEquipmentLocation(Location location);
    
    List<EquipmentUnit> findByStatus(EquipmentUnit.UnitStatus status);
    
    List<EquipmentUnit> findByEquipmentLocationAndStatus(Location location, EquipmentUnit.UnitStatus status);
    
    Optional<EquipmentUnit> findBySerialNumber(String serialNumber);
    
    boolean existsBySerialNumber(String serialNumber);
    
    @Query("SELECT COUNT(eu) FROM EquipmentUnit eu WHERE eu.equipment.location = :location")
    Long countByLocation(@Param("location") Location location);
    
    @Query("SELECT COUNT(eu) FROM EquipmentUnit eu WHERE eu.equipment.location = :location AND eu.status = :status")
    Long countByLocationAndStatus(@Param("location") Location location, @Param("status") EquipmentUnit.UnitStatus status);
    
    @Query("SELECT eu FROM EquipmentUnit eu WHERE eu.equipment.location = :location AND eu.status = 'AVAILABLE'")
    List<EquipmentUnit> findAvailableByLocation(@Param("location") Location location);
    
    @Query("SELECT eu FROM EquipmentUnit eu WHERE eu.equipment = :equipment AND eu.status = 'AVAILABLE'")
    List<EquipmentUnit> findAvailableByEquipment(@Param("equipment") Equipment equipment);

    @Query("SELECT COUNT(eu) FROM EquipmentUnit eu WHERE eu.equipment.location = :location")
    int countByEquipmentLocation(@Param("location") Location location);
    
    @Query("SELECT COUNT(eu) FROM EquipmentUnit eu WHERE eu.equipment.location = :location AND eu.status = :status")
    int countByEquipmentLocationAndStatus(@Param("location") Location location, 
                                         @Param("status") EquipmentUnit.UnitStatus status);
    
    @Query("SELECT COUNT(eu) FROM EquipmentUnit eu WHERE eu.status = :status")
    int countByStatus(@Param("status") EquipmentUnit.UnitStatus status);
    
    @Query("SELECT COUNT(eu) FROM EquipmentUnit eu WHERE eu.equipment = :equipment")
    int countByEquipment(@Param("equipment") Equipment equipment);
    
    @Query("SELECT COUNT(eu) FROM EquipmentUnit eu WHERE eu.equipment = :equipment AND eu.status = :status")
    int countByEquipmentAndStatus(@Param("equipment") Equipment equipment, 
                                 @Param("status") EquipmentUnit.UnitStatus status);
}