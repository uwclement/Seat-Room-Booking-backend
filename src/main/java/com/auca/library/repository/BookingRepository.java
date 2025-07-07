package com.auca.library.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.auca.library.model.Booking;
import com.auca.library.model.Booking.BookingStatus;
import com.auca.library.model.Seat;
import com.auca.library.model.User;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {
    List<Booking> findByUser(User user);
    
    List<Booking> findBySeat(Seat seat);
    
    @Query("SELECT b FROM Booking b WHERE b.user.id = ?1 AND b.status IN ('RESERVED', 'CHECKED_IN')")
    List<Booking> findActiveBookingsByUserId(Long userId);
    
    // @Query("SELECT b FROM Booking b WHERE b.seat.id = ?1 AND b.endTime > ?2 AND b.startTime < ?3 AND b.status IN ('RESERVED', 'CHECKED_IN')")
    // List<Booking> findOverlappingBookings(Long seatId, LocalDateTime start, LocalDateTime end);
    
    @Query("SELECT b FROM Booking b WHERE b.status = 'RESERVED' AND b.startTime BETWEEN ?1 AND ?2")
    List<Booking> findUpcomingReservations(LocalDateTime start, LocalDateTime end);
    
    @Query("SELECT b FROM Booking b WHERE b.user.id = ?1 AND b.startTime > ?2 ORDER BY b.startTime ASC")
    List<Booking> findFutureBookingsByUser(Long userId, LocalDateTime now);
    
    @Query("SELECT b FROM Booking b WHERE b.user.id = ?1 AND b.endTime < ?2 ORDER BY b.startTime DESC")
    List<Booking> findPastBookingsByUser(Long userId, LocalDateTime now);
    
    @Query("SELECT b FROM Booking b WHERE b.endTime > ?1 AND b.endTime < ?2 AND b.extended = false AND b.status = 'CHECKED_IN'")
    List<Booking> findBookingsNearingCompletion(LocalDateTime start, LocalDateTime end);


     @Query("SELECT b FROM Booking b WHERE b.seat.id = :seatId AND " +
           "((b.startTime <= :endTime AND b.endTime >= :startTime) OR " + 
           "(b.startTime >= :startTime AND b.startTime <= :endTime)) AND " +
           "b.status IN ('RESERVED', 'CHECKED_IN')")

    List<Booking> findOverlappingBookings(@Param("seatId") Long seatId, 
                                        @Param("startTime") LocalDateTime startTime, 
                                        @Param("endTime") LocalDateTime endTime);
    
    List<Booking> findByStartTimeBetween(LocalDateTime start, LocalDateTime end);
    
    List<Booking> findByEndTimeAfterAndStartTimeBeforeAndStatusIn(
            LocalDateTime endTimeAfter, 
            LocalDateTime startTimeBefore, 
            List<BookingStatus> statuses);
    
    List<Booking> findByUserId(Long userId);
    
    List<Booking> findBySeatId(Long seatId);



// Find bookings that started more than 20 minutes ago and haven't been checked in

    @Query("SELECT b FROM Booking b WHERE b.startTime < :cutoffTime AND b.status = 'RESERVED' AND b.checkedIn = false")
    List<Booking> findNoShowBookings(@Param("cutoffTime") LocalDateTime cutoffTime, @Param("now") LocalDateTime now);


 // Find bookings that started between 10 and 19 minutes ago and haven't been checked in

   @Query("SELECT b FROM Booking b WHERE " +
       "b.checkedIn = false AND " +
       "b.warningSent = false AND " +
       "b.startTime BETWEEN :maxCutoff AND :warningCutoff")
   List<Booking> findBookingsNeedingWarning( @Param("warningCutoff") LocalDateTime warningCutoff, @Param("maxCutoff") LocalDateTime maxCutoff);



  // Find bookings eligible for manual check-in (Reserved bookings within the check-in time window)
 

@Query("SELECT b FROM Booking b WHERE b.status = :status AND " +
       "b.startTime BETWEEN :earliestCheckIn AND :latestCheckIn " +
       "ORDER BY b.startTime ASC")
List<Booking> findBookingsEligibleForCheckIn(
    @Param("earliestCheckIn") LocalDateTime earliestCheckIn,
    @Param("latestCheckIn") LocalDateTime latestCheckIn,
    @Param("status") BookingStatus status
);


 // Find bookings by status list
@Query("SELECT b FROM Booking b WHERE b.status IN :statuses ORDER BY b.startTime ASC")
List<Booking> findByStatusIn(@Param("statuses") List<BookingStatus> statuses);


 // Find active bookings for today (for admin dashboard)

@Query("SELECT b FROM Booking b WHERE b.startTime >= :startOfDay AND b.startTime < :endOfDay AND " +
       "b.status IN :statuses ORDER BY b.startTime ASC")
List<Booking> findTodaysActiveBookings(
    @Param("startOfDay") LocalDateTime startOfDay,
    @Param("endOfDay") LocalDateTime endOfDay,
    @Param("statuses") List<BookingStatus> statuses
);

 // Count bookings by status for admin statistics

@Query("SELECT b.status, COUNT(b) FROM Booking b WHERE b.startTime >= :startOfDay AND b.startTime < :endOfDay GROUP BY b.status")
List<Object[]> countTodaysBookingsByStatus(
    @Param("startOfDay") LocalDateTime startOfDay,
    @Param("endOfDay") LocalDateTime endOfDay
);


@Query("SELECT b FROM Booking b WHERE b.startTime BETWEEN :startDateTime AND :endDateTime ORDER BY b.startTime ASC")
List<Booking> findBookingsByDateRange(
    @Param("startDateTime") LocalDateTime startDateTime,
    @Param("endDateTime") LocalDateTime endDateTime
);


 // Find bookings by date (using LocalDateTime parameters)

@Query("SELECT b FROM Booking b WHERE b.startTime >= :startOfDay AND b.startTime < :startOfNextDay ORDER BY b.startTime ASC")
List<Booking> findBookingsByDate(
    @Param("startOfDay") LocalDateTime startOfDay,
    @Param("startOfNextDay") LocalDateTime startOfNextDay
);

}

