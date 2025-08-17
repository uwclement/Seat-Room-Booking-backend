package com.auca.library.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.auca.library.model.Equipment;
import com.auca.library.model.Location;

@Repository
public interface EquipmentRepository extends JpaRepository<Equipment, Long> {
    
    Optional<Equipment> findByName(String name);
    
    boolean existsByName(String name);
    
    List<Equipment> findByAvailableTrue();
    
    List<Equipment> findByAvailableFalse();
    
    // NEW: Location-based queries
    List<Equipment> findByLocation(Location location);
    
    List<Equipment> findByLocationAndAvailableTrue(Location location);
    
    @Query("SELECT e FROM Equipment e WHERE LOWER(e.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(e.description) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<Equipment> searchEquipment(@Param("keyword") String keyword);
    
    @Query("SELECT e FROM Equipment e WHERE e.location = :location AND " +
           "(LOWER(e.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(e.description) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    List<Equipment> searchEquipmentByLocation(@Param("keyword") String keyword, @Param("location") Location location);
    
    // Find equipment used by rooms
    @Query("SELECT DISTINCT e FROM Equipment e JOIN e.rooms r WHERE r.id = :roomId")
    List<Equipment> findByRoomId(@Param("roomId") Long roomId);
    
    // Find unused equipment
    @Query("SELECT e FROM Equipment e WHERE e.rooms IS EMPTY")
    List<Equipment> findUnusedEquipment();
    
    // Statistics
    @Query("SELECT COUNT(e) FROM Equipment e WHERE e.available = true")
    Long countAvailableEquipment();

    List<Equipment> findByAvailable(boolean available);

    List<Equipment> findByAllowedToStudentsTrue();
    
    @Query("SELECT e FROM Equipment e WHERE e.allowedToStudents = true AND e.available = true")
    List<Equipment> findStudentAllowedAndAvailable();
    
    @Query("SELECT e FROM Equipment e WHERE e.allowedToStudents = true AND e.location = :location AND e.available = true")
    List<Equipment> findStudentAllowedAndAvailableByLocation(@Param("location") Location location);
    
    @Query("SELECT e FROM Equipment e WHERE e.availableQuantity < :threshold")
    List<Equipment> findLowInventoryEquipment(@Param("threshold") Integer threshold);
    
    @Query("SELECT e FROM Equipment e WHERE e.availableQuantity = 0 AND e.available = true")
    List<Equipment> findOutOfStockEquipment();
    
    @Query("SELECT DISTINCT e FROM Equipment e JOIN e.labClasses l WHERE l.id = :labId")
    List<Equipment> findByLabClassId(@Param("labId") Long labId);
    
    @Query("SELECT e FROM Equipment e WHERE e.rooms IS EMPTY AND e.labClasses IS EMPTY")
    List<Equipment> findUnassignedEquipment();
    
    @Query("SELECT COUNT(e) FROM Equipment e WHERE e.allowedToStudents = true")
    Long countStudentAllowedEquipment();
     
    @Query("SELECT COUNT(e) FROM Equipment e WHERE e.location = :location ")
    int countByLocation(@Param("location") Location location);
    
}