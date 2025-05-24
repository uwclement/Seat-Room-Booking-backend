package com.auca.library.repository;

import com.auca.library.model.Equipment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EquipmentRepository extends JpaRepository<Equipment, Long> {
    
    Optional<Equipment> findByName(String name);
    
    boolean existsByName(String name);
    
    List<Equipment> findByAvailableTrue();
    
    List<Equipment> findByAvailableFalse();
    
    @Query("SELECT e FROM Equipment e WHERE LOWER(e.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(e.description) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<Equipment> searchEquipment(@Param("keyword") String keyword);
    
    // Find equipment used by rooms
    @Query("SELECT DISTINCT e FROM Equipment e JOIN e.rooms r WHERE r.id = :roomId")
    List<Equipment> findByRoomId(@Param("roomId") Long roomId);
    
    // Find unused equipment
    @Query("SELECT e FROM Equipment e WHERE e.rooms IS EMPTY")
    List<Equipment> findUnusedEquipment();
    
    // Statistics
    @Query("SELECT COUNT(e) FROM Equipment e WHERE e.available = true")
    Long countAvailableEquipment();
}