package com.auca.library.repository;

import com.auca.library.model.LabClass;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface LabClassRepository extends JpaRepository<LabClass, Long> {
    
    Optional<LabClass> findByLabNumber(String labNumber);
    
    boolean existsByLabNumber(String labNumber);
    
    List<LabClass> findByAvailableTrue();
    
    List<LabClass> findByBuilding(String building);
    
    @Query("SELECT l FROM LabClass l WHERE LOWER(l.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(l.labNumber) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<LabClass> searchLabClasses(@Param("keyword") String keyword);
    
    // Check if lab is available during specified time
    @Query("SELECT CASE WHEN COUNT(er) = 0 THEN true ELSE false END " +
           "FROM EquipmentRequest er " +
           "WHERE er.labClass.id = :labId " +
           "AND er.status IN ('APPROVED', 'HOD_APPROVED') " +
           "AND ((er.startTime <= :endTime AND er.endTime >= :startTime))")
    boolean isLabAvailable(@Param("labId") Long labId, 
                          @Param("startTime") LocalDateTime startTime, 
                          @Param("endTime") LocalDateTime endTime);
}