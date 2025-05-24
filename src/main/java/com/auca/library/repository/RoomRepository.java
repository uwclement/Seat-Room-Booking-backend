package com.auca.library.repository;

import com.auca.library.model.Room;
import com.auca.library.model.RoomCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface RoomRepository extends JpaRepository<Room, Long> {
    
    // Basic queries
    Optional<Room> findByRoomNumber(String roomNumber);
    
    boolean existsByRoomNumber(String roomNumber);
    
    List<Room> findByCategory(RoomCategory category);
    
    List<Room> findByAvailableTrue();
    
    List<Room> findByAvailableFalse();
    
    // Location-based queries
    List<Room> findByBuilding(String building);
    
    List<Room> findByBuildingAndFloor(String building, String floor);
    
    List<Room> findByDepartment(String department);
    
    // Capacity-based queries
    List<Room> findByCapacityGreaterThanEqual(Integer minCapacity);
    
    List<Room> findByCapacityBetween(Integer minCapacity, Integer maxCapacity);
    
    // Category and availability queries
    List<Room> findByCategoryAndAvailableTrue(RoomCategory category);
    
    // Equipment-based queries
    @Query("SELECT DISTINCT r FROM Room r JOIN r.equipment e WHERE e.id IN :equipmentIds")
    List<Room> findByEquipmentIds(@Param("equipmentIds") List<Long> equipmentIds);
    
    @Query("SELECT r FROM Room r JOIN r.equipment e WHERE e.name = :equipmentName")
    List<Room> findByEquipmentName(@Param("equipmentName") String equipmentName);
    
    // Maintenance queries
    @Query("SELECT r FROM Room r WHERE r.maintenanceStart IS NOT NULL AND r.maintenanceEnd IS NOT NULL " +
           "AND :now BETWEEN r.maintenanceStart AND r.maintenanceEnd")
    List<Room> findRoomsUnderMaintenance(@Param("now") LocalDateTime now);
    
    @Query("SELECT r FROM Room r WHERE r.maintenanceStart IS NOT NULL AND r.maintenanceEnd IS NOT NULL " +
           "AND r.maintenanceEnd > :now")
    List<Room> findRoomsWithScheduledMaintenance(@Param("now") LocalDateTime now);
    
    // Booking configuration queries
    List<Room> findByRequiresApprovalTrue();
    
    List<Room> findByMaxBookingHoursGreaterThanEqual(Integer minHours);
    
    // Search queries
    @Query("SELECT r FROM Room r WHERE " +
           "LOWER(r.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(r.roomNumber) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(r.description) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<Room> searchRooms(@Param("keyword") String keyword);
    
    // Complex filtering query
    @Query("SELECT r FROM Room r WHERE " +
           "(:category IS NULL OR r.category = :category) AND " +
           "(:available IS NULL OR r.available = :available) AND " +
           "(:minCapacity IS NULL OR r.capacity >= :minCapacity) AND " +
           "(:maxCapacity IS NULL OR r.capacity <= :maxCapacity) AND " +
           "(:building IS NULL OR LOWER(r.building) = LOWER(:building)) AND " +
           "(:floor IS NULL OR LOWER(r.floor) = LOWER(:floor)) AND " +
           "(:department IS NULL OR LOWER(r.department) = LOWER(:department))")
    List<Room> findRoomsWithFilters(
            @Param("category") RoomCategory category,
            @Param("available") Boolean available,
            @Param("minCapacity") Integer minCapacity,
            @Param("maxCapacity") Integer maxCapacity,
            @Param("building") String building,
            @Param("floor") String floor,
            @Param("department") String department
    );
    
    // Statistics queries
    @Query("SELECT COUNT(r) FROM Room r WHERE r.category = :category")
    Long countByCategory(@Param("category") RoomCategory category);
    
    @Query("SELECT COUNT(r) FROM Room r WHERE r.available = true")
    Long countAvailableRooms();
    
    @Query("SELECT r.building, COUNT(r) FROM Room r GROUP BY r.building")
    List<Object[]> countRoomsByBuilding();
    
    // Admin utility queries
    @Query("SELECT r FROM Room r WHERE r.updatedAt >= :since ORDER BY r.updatedAt DESC")
    List<Room> findRecentlyUpdatedRooms(@Param("since") LocalDateTime since);
    
    @Query("SELECT r FROM Room r ORDER BY r.createdAt DESC")
    List<Room> findAllOrderByCreatedAtDesc();
}