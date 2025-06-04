package com.auca.library.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.auca.library.dto.response.RecurringSeriesStats;
import com.auca.library.model.RecurringBookingSeries;
import com.auca.library.model.Room;
import com.auca.library.model.RoomBooking;
import com.auca.library.model.RoomCategory;
import com.auca.library.model.User;

@Repository
public interface RoomBookingRepository extends JpaRepository<RoomBooking, Long> {
    
    // ========== REAL-TIME AVAILABILITY QUERIES ==========
    
    @Query("SELECT rb FROM RoomBooking rb WHERE rb.room = :room AND " +
           "rb.status IN ('CONFIRMED', 'CHECKED_IN') AND " +
           "rb.endTime > :now AND rb.startTime < :endTime")
    List<RoomBooking> findActiveBookingsForRoom(@Param("room") Room room, 
                                               @Param("now") LocalDateTime now, 
                                               @Param("endTime") LocalDateTime endTime);
    
    @Query("SELECT rb FROM RoomBooking rb WHERE rb.room = :room AND " +
           "rb.status IN ('CONFIRMED', 'CHECKED_IN') AND " +
           "rb.startTime >= :weekStart AND rb.startTime < :weekEnd " +
           "ORDER BY rb.startTime")
    List<RoomBooking> findWeeklyBookingsForRoom(@Param("room") Room room,
                                               @Param("weekStart") LocalDateTime weekStart,
                                               @Param("weekEnd") LocalDateTime weekEnd);
    
    @Query("SELECT rb FROM RoomBooking rb WHERE rb.room = :room AND " +
           "rb.status IN ('CONFIRMED', 'CHECKED_IN') AND " +
           "rb.startTime <= :now AND rb.endTime > :now")
    Optional<RoomBooking> findCurrentBookingForRoom(@Param("room") Room room, @Param("now") LocalDateTime now);
    
    @Query("SELECT rb FROM RoomBooking rb WHERE rb.room = :room AND " +
           "rb.status IN ('PENDING', 'CONFIRMED') AND " +
           "rb.startTime > :now ORDER BY rb.startTime LIMIT 1")
    Optional<RoomBooking> findNextBookingForRoom(@Param("room") Room room, @Param("now") LocalDateTime now);
    
    @Query("SELECT rb FROM RoomBooking rb WHERE rb.room = :room AND " +
           "rb.status IN ('PENDING', 'CONFIRMED') AND " +
           "rb.startTime >= :startTime AND rb.startTime <= :endTime ORDER BY rb.startTime")
    List<RoomBooking> findUpcomingBookingsForRoom(@Param("room") Room room, 
                                                 @Param("startTime") LocalDateTime startTime, 
                                                 @Param("endTime") LocalDateTime endTime);
    
    @Query("SELECT MIN(rb.endTime) FROM RoomBooking rb WHERE rb.room = :room AND " +
           "rb.status IN ('CONFIRMED', 'CHECKED_IN') AND rb.endTime > :now")
    Optional<LocalDateTime> findNextAvailableTime(@Param("room") Room room, @Param("now") LocalDateTime now);
    
    // ========== CONFLICT DETECTION ==========
    
    @Query("SELECT COUNT(rb) FROM RoomBooking rb WHERE rb.room = :room " +
           "AND rb.status IN ('PENDING', 'CONFIRMED', 'CHECKED_IN') " +
           "AND ((rb.startTime <= :startTime AND rb.endTime > :startTime) " +
           "OR (rb.startTime < :endTime AND rb.endTime >= :endTime) " +
           "OR (rb.startTime >= :startTime AND rb.endTime <= :endTime))")
    Long countConflictingBookings(@Param("room") Room room, 
                                 @Param("startTime") LocalDateTime startTime,
                                 @Param("endTime") LocalDateTime endTime);
    
    @Query("SELECT COUNT(rb) FROM RoomBooking rb WHERE rb.room = :room " +
           "AND rb.status IN ('PENDING', 'CONFIRMED', 'CHECKED_IN') " +
           "AND rb.id != :excludeBookingId " +
           "AND ((rb.startTime <= :startTime AND rb.endTime > :startTime) " +
           "OR (rb.startTime < :endTime AND rb.endTime >= :endTime) " +
           "OR (rb.startTime >= :startTime AND rb.endTime <= :endTime))")
    Long countConflictingBookingsExcluding(@Param("room") Room room, 
                                          @Param("startTime") LocalDateTime startTime,
                                          @Param("endTime") LocalDateTime endTime, 
                                          @Param("excludeBookingId") Long excludeBookingId);
    
    // ========== USER BOOKING LIMITS ==========
    
    @Query("SELECT COUNT(rb) FROM RoomBooking rb WHERE rb.user = :user AND " +
           "rb.status IN ('CONFIRMED', 'CHECKED_IN') AND " +
           "DATE(rb.startTime) = DATE(:date)")
    Long countUserBookingsForDate(@Param("user") User user, @Param("date") LocalDateTime date);
    
    @Query("SELECT COUNT(rb) FROM RoomBooking rb WHERE rb.user = :user AND " +
           "rb.status IN ('CONFIRMED', 'CHECKED_IN') AND " +
           "rb.startTime >= :weekStart AND rb.startTime < :weekEnd")
    Long countUserBookingsForWeek(@Param("user") User user,
                                 @Param("weekStart") LocalDateTime weekStart,
                                 @Param("weekEnd") LocalDateTime weekEnd);
    
    // ========== CHECK-IN QUERIES ==========
    
    @Query("SELECT rb FROM RoomBooking rb WHERE rb.status = 'CONFIRMED' AND " +
           "rb.checkedInAt IS NULL AND rb.startTime <= :checkTime")
    List<RoomBooking> findBookingsRequiringCheckIn(@Param("checkTime") LocalDateTime checkTime);
    
    @Query("SELECT rb FROM RoomBooking rb WHERE rb.status = 'CONFIRMED' AND " +
           "rb.checkedInAt IS NULL AND rb.startTime <= :overdueTime")
    List<RoomBooking> findOverdueBookings(@Param("overdueTime") LocalDateTime overdueTime);
    
    @Query("SELECT rb FROM RoomBooking rb WHERE rb.status = 'CONFIRMED' AND rb.checkedInAt IS NULL " +
           "AND rb.startTime < :cutoffTime AND rb.endTime > CURRENT_TIMESTAMP")
    List<RoomBooking> findNoShowBookings(@Param("cutoffTime") LocalDateTime cutoffTime);
    
    // ========== REMINDER QUERIES ==========
    
    @Query("SELECT rb FROM RoomBooking rb WHERE rb.status = 'CONFIRMED' AND " +
           "rb.reminderSentAt IS NULL AND rb.reminderEnabled = true AND " +
           "rb.startTime BETWEEN :reminderTime AND :maxReminderTime")
    List<RoomBooking> findBookingsNeedingReminders(@Param("reminderTime") LocalDateTime reminderTime,
                                                   @Param("maxReminderTime") LocalDateTime maxReminderTime);
    
    // ========== PUBLIC/JOINABLE BOOKINGS ==========
    
    @Query("SELECT rb FROM RoomBooking rb WHERE rb.isPublic = true AND " +
           "rb.allowJoining = true AND rb.status = 'CONFIRMED' AND " +
           "rb.startTime > :now AND " +
           "(SELECT COUNT(p) FROM BookingParticipant p WHERE p.booking = rb AND p.status = 'ACCEPTED') < rb.maxParticipants")
    List<RoomBooking> findJoinableBookings(@Param("now") LocalDateTime now);
    
    // ========== USER HISTORY ==========
    
    @Query("SELECT rb FROM RoomBooking rb WHERE rb.user = :user AND " +
           "rb.status IN ('COMPLETED', 'NO_SHOW', 'CANCELLED') " +
           "ORDER BY rb.startTime DESC")
    List<RoomBooking> findUserBookingHistory(@Param("user") User user);
    
    @Query("SELECT rb FROM RoomBooking rb WHERE rb.user = :user AND rb.startTime > :startTime")
    List<RoomBooking> findByUserAndStartTimeAfter(@Param("user") User user, @Param("startTime") LocalDateTime startTime);
    
    // ========== APPROVAL QUERIES ==========
    
    @Query("SELECT rb FROM RoomBooking rb WHERE rb.status = 'PENDING' AND " +
           "rb.requiresApproval = true ORDER BY rb.createdAt ASC")
    List<RoomBooking> findPendingApprovals();
    
    
    @Query("SELECT rb FROM RoomBooking rb WHERE rb.status = 'PENDING' AND rb.requiresApproval = true AND rb.createdAt < :cutoffTime")
    List<RoomBooking> findOverduePendingApprovals(@Param("cutoffTime") LocalDateTime cutoffTime);
    
    @Query("SELECT rb FROM RoomBooking rb WHERE rb.approvedBy IS NOT NULL " +
           "AND rb.approvedAt BETWEEN :startDate AND :endDate ORDER BY rb.approvedAt DESC")
    List<RoomBooking> findApprovalHistory(@Param("startDate") LocalDateTime startDate, 
                                         @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT rb FROM RoomBooking rb WHERE rb.approvedBy = :admin " +
           "AND rb.approvedAt BETWEEN :startDate AND :endDate ORDER BY rb.approvedAt DESC")
    List<RoomBooking> findApprovalHistoryByAdmin(@Param("admin") User admin,
                                               @Param("startDate") LocalDateTime startDate, 
                                               @Param("endDate") LocalDateTime endDate);
    
    // ========== RECURRING BOOKING QUERIES ==========
    
    @Query("SELECT rb FROM RoomBooking rb WHERE rb.recurringBookingSeries = :series")
    List<RoomBooking> findByRecurringBookingSeries(@Param("series") RecurringBookingSeries series);
    
    @Query("SELECT rb FROM RoomBooking rb WHERE rb.recurringBookingSeries = :series AND rb.startTime > :startTime")
    List<RoomBooking> findByRecurringSeriesAndStartTimeAfter(
        @Param("series") RecurringBookingSeries series, 
        @Param("startTime") LocalDateTime startTime
    );
    
    @Query("SELECT COUNT(rb) FROM RoomBooking rb WHERE rb.recurringBookingSeries = :series AND rb.status = :status")
    Long countByRecurringSeriesAndStatus(
        @Param("series") RecurringBookingSeries series, 
        @Param("status") RoomBooking.BookingStatus status
    );
    
    @Query("SELECT new com.auca.library.dto.response.RecurringSeriesStats(" +
           "COUNT(rb), " +
           "SUM(CASE WHEN rb.status = 'COMPLETED' THEN 1 ELSE 0 END), " +
           "SUM(CASE WHEN rb.status = 'CANCELLED' THEN 1 ELSE 0 END), " +
           "SUM(CASE WHEN rb.status = 'NO_SHOW' THEN 1 ELSE 0 END)) " +
           "FROM RoomBooking rb WHERE rb.recurringBookingSeries = :series")
    RecurringSeriesStats getRecurringSeriesStats(@Param("series") RecurringBookingSeries series);
    
    // ========== PERIOD QUERIES ==========
    
    @Query("SELECT rb FROM RoomBooking rb WHERE rb.room = :room AND rb.startTime >= :startTime AND rb.endTime <= :endTime AND rb.status != 'CANCELLED' ORDER BY rb.startTime")
    List<RoomBooking> findByStartTimeBetween(@Param("room") Room room, 
                                          @Param("startTime") LocalDateTime startTime, 
                                          @Param("endTime") LocalDateTime endTime);
    
    @Query("SELECT rb FROM RoomBooking rb WHERE rb.status IN ('CONFIRMED', 'CHECKED_IN') AND rb.startTime <= :now AND rb.endTime > :now")
    List<RoomBooking> findCurrentlyActiveBookings(@Param("now") LocalDateTime now);
    
    // ========== ANALYTICS AND STATISTICS ==========
    
    @Query("SELECT rb.room, COUNT(rb) as bookingCount FROM RoomBooking rb WHERE " +
           "rb.startTime >= :startDate AND rb.startTime < :endDate AND " +
           "rb.status IN ('COMPLETED', 'CHECKED_IN') " +
           "GROUP BY rb.room ORDER BY bookingCount DESC")
    List<Object[]> findMostUsedRooms(@Param("startDate") LocalDateTime startDate,
                                    @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT DATE(rb.startTime), COUNT(rb) FROM RoomBooking rb WHERE " +
           "rb.room = :room AND rb.startTime >= :startDate AND rb.startTime < :endDate " +
           "GROUP BY DATE(rb.startTime) ORDER BY DATE(rb.startTime)")
    List<Object[]> findDailyBookingCounts(@Param("room") Room room,
                                         @Param("startDate") LocalDateTime startDate,
                                         @Param("endDate") LocalDateTime endDate);
    
   @Query("SELECT u.id, u.fullName, COUNT(rb.id) as bookingCount " +
       "FROM RoomBooking rb JOIN rb.user u " +
       "WHERE rb.startTime >= :startDate " +
       "GROUP BY u.id, u.fullName " +
       "ORDER BY bookingCount DESC")
List<Object[]> findUserBookingStatistics(@Param("startDate") LocalDateTime startDate);
    
@Query("SELECT r.id, r.name, r.building, COUNT(rb.id) as bookingCount " +
       "FROM Room r LEFT JOIN RoomBooking rb ON r.id = rb.room.id " +
       "AND rb.startTime >= :startDate AND rb.status IN ('CONFIRMED', 'COMPLETED', 'CHECKED_IN') " +
       "WHERE r.available = true " +
       "GROUP BY r.id, r.name, r.building " +
       "ORDER BY bookingCount DESC")
List<Object[]> findRoomUtilizationStatistics(@Param("startDate") LocalDateTime startDate);

    
    @Query("SELECT EXTRACT(HOUR FROM rb.startTime) as hour, COUNT(rb.id) as bookingCount " +
           "FROM RoomBooking rb " +
           "WHERE rb.startTime >= :startDate " +
           "GROUP BY EXTRACT(HOUR FROM rb.startTime) " +
           "ORDER BY bookingCount DESC")
    List<Object[]> findPeakHours(@Param("startDate") LocalDateTime startDate);
    
    @Query("SELECT DATE(rb.startTime) as bookingDate, " +
           "COUNT(rb.id) as totalBookings, " +
           "COUNT(CASE WHEN rb.status = 'CONFIRMED' THEN 1 END) as confirmedBookings, " +
           "COUNT(CASE WHEN rb.status = 'CANCELLED' THEN 1 END) as cancelledBookings, " +
           "COUNT(DISTINCT rb.user.id) as uniqueUsers, " +
           "COUNT(DISTINCT rb.room.id) as uniqueRooms " +
           "FROM RoomBooking rb " +
           "WHERE rb.startTime BETWEEN :startDate AND :endDate " +
           "GROUP BY DATE(rb.startTime) " +
           "ORDER BY bookingDate")
    List<Object[]> findDailyBookingStatistics(@Param("startDate") LocalDateTime startDate, 
                                             @Param("endDate") LocalDateTime endDate);
    
@Query("SELECT AVG(TIMESTAMPDIFF(HOUR, rb.createdAt, rb.approvedAt)) " +
       "FROM RoomBooking rb " +
       "WHERE rb.approvedAt IS NOT NULL AND rb.createdAt >= :startDate")
Double findAverageApprovalTimeHours(@Param("startDate") LocalDateTime startDate);
    
    @Query("SELECT e.name, COUNT(rb.id) as usageCount " +
           "FROM RoomBooking rb JOIN rb.requestedEquipment e " +
           "WHERE rb.startTime >= :startDate " +
           "GROUP BY e.name " +
           "ORDER BY usageCount DESC")
    List<Object[]> findEquipmentUsageStatistics(@Param("startDate") LocalDateTime startDate);
    
    // ========== SIMPLE FINDER METHODS ==========
    
    List<RoomBooking> findByUserAndStatusIn(User user, List<RoomBooking.BookingStatus> statuses);
    
    List<RoomBooking> findByRoomAndStatusIn(Room room, List<RoomBooking.BookingStatus> statuses);
    
    List<RoomBooking> findByUserAndStartTimeBetweenAndStatusIn(User user, LocalDateTime startTime, LocalDateTime endTime, List<RoomBooking.BookingStatus> statuses);
    
    List<RoomBooking> findByStatusAndRequiresApprovalTrue(RoomBooking.BookingStatus status);
    
    // ========== COMPLEX FILTERING WITH PAGINATION ==========
    
    @Query("SELECT rb FROM RoomBooking rb JOIN rb.room r JOIN rb.user u WHERE " +
           "(:status IS NULL OR rb.status = :status) AND " +
           "(:roomId IS NULL OR r.id = :roomId) AND " +
           "(:userId IS NULL OR u.id = :userId) AND " +
           "(:startDate IS NULL OR rb.startTime >= :startDate) AND " +
           "(:endDate IS NULL OR rb.startTime <= :endDate) AND " +
           "(:building IS NULL OR LOWER(r.building) = LOWER(:building)) AND " +
           "(:requiresApproval IS NULL OR rb.requiresApproval = :requiresApproval) AND " +
           "(:search IS NULL OR LOWER(rb.title) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(rb.description) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(u.fullName) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<RoomBooking> findBookingsWithFilters(
            @Param("status") RoomBooking.BookingStatus status,
            @Param("roomId") Long roomId,
            @Param("userId") Long userId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("building") String building,
            @Param("requiresApproval") Boolean requiresApproval,
            @Param("search") String search,
            Pageable pageable);

    List<RoomBooking> findAll();
    List<RoomBooking> findByStartTimeBetween(LocalDateTime start, LocalDateTime end);

    @Query("SELECT rb FROM RoomBooking rb WHERE rb.status = 'PENDING'")
    List<RoomBooking> findPendingApprovalBookings();

    @Query("SELECT DATE(rb.startTime) as bookingDate, " +
           "COUNT(rb.id) as totalBookings, " +
           "COUNT(CASE WHEN rb.status = 'CONFIRMED' THEN 1 END) as confirmedBookings, " +
           "COUNT(CASE WHEN rb.status = 'CANCELLED' THEN 1 END) as cancelledBookings, " +
           "COUNT(CASE WHEN rb.status = 'NO_SHOW' THEN 1 END) as noShowBookings, " +
           "COUNT(CASE WHEN rb.status = 'COMPLETED' THEN 1 END) as completedBookings " +
           "FROM RoomBooking rb " +
           "WHERE rb.startTime >= :startDate AND rb.startTime < :endDate " +
           "GROUP BY DATE(rb.startTime) " +
           "ORDER BY bookingDate")
    List<Object[]> findDailyBookingStatusCounts(@Param("startDate") LocalDateTime startDate,
                                                @Param("endDate") LocalDateTime endDate);

    // ========== FIXED: REMOVE THE PROBLEMATIC ROOM METHODS ==========
    // These methods should be in RoomRepository, not RoomBookingRepository
    // REMOVE THESE METHODS:
    // List<Room> findByAvailableAndRequiresBooking(boolean available, boolean requiresBooking);
    // List<Room> findByAvailableAndCategoryAndRequiresBooking(boolean available, RoomCategory category, boolean requiresBooking);
    // @Query("SELECT r FROM Room r WHERE r.available = true AND r.requiresBooking = true AND r.underMaintenance = false")
    // List<Room> findAvailableRoomsForBooking();

    // Add these to your RoomBookingRepository
@Query("SELECT rb FROM RoomBooking rb WHERE rb.room = :room AND rb.startTime >= :startTime AND rb.startTime <= :endTime ORDER BY rb.startTime")
List<RoomBooking> findBookingsInPeriod(@Param("room") Room room, 
                                      @Param("startTime") LocalDateTime startTime, 
                                      @Param("endTime") LocalDateTime endTime);

@Query("SELECT rb FROM RoomBooking rb WHERE rb.startTime >= :startTime AND rb.startTime <= :endTime ORDER BY rb.startTime")
List<RoomBooking> findBookingsInPeriod(@Param("startTime") LocalDateTime startTime, 
                                      @Param("endTime") LocalDateTime endTime);
}