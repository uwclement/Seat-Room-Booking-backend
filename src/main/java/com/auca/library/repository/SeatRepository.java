package com.auca.library.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.auca.library.model.Seat;

@Repository
public interface SeatRepository extends JpaRepository<Seat, Long> {
    List<Seat> findByZoneType(String zoneType);
    
    List<Seat> findByHasDesktop(boolean hasDesktop);
    
    @Query("SELECT s FROM Seat s WHERE s.zoneType = ?1 AND s.hasDesktop = ?2")
    List<Seat> findByZoneTypeAndHasDesktop(String zoneType, boolean hasDesktop);
    
    List<Seat> findByIsDisabled(boolean isDisabled);
    

    // QR Code methods
    Optional<Seat> findByQrCodeToken(String qrCodeToken);
    boolean existsByQrCodeToken(String qrCodeToken);
    List<Seat> findByQrCodeTokenIsNull();
    
    @Query("SELECT s FROM Seat s WHERE s.qrImagePath IS NULL AND s.isDisabled = false")
    List<Seat> findSeatsWithoutQRCode();


    @Query("SELECT s FROM Seat s WHERE s.isDisabled = false")
    List<Seat> findAllEnabledSeats();


    @Query("SELECT s FROM Seat s WHERE s.qrImagePath IS NOT NULL AND s.isDisabled = false")
    List<Seat> findSeatsWithQRCodePath();
    
}