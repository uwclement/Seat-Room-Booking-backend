package com.auca.library.repository;

import com.auca.library.model.Seat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SeatRepository extends JpaRepository<Seat, Long> {
    List<Seat> findByZoneType(String zoneType);
    
    List<Seat> findByHasDesktop(boolean hasDesktop);
    
    @Query("SELECT s FROM Seat s WHERE s.zoneType = ?1 AND s.hasDesktop = ?2")
    List<Seat> findByZoneTypeAndHasDesktop(String zoneType, boolean hasDesktop);
    
    List<Seat> findByIsDisabled(boolean isDisabled);
}