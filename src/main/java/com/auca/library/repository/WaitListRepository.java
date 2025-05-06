package com.auca.library.repository;

import com.auca.library.model.Seat;
import com.auca.library.model.User;
import com.auca.library.model.WaitList;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface WaitListRepository extends JpaRepository<WaitList, Long> {
    List<WaitList> findByUserAndStatusOrderByCreatedAtDesc(User user, WaitList.WaitListStatus status);
    
    List<WaitList> findBySeatAndStatusOrderByQueuePositionAsc(Seat seat, WaitList.WaitListStatus status);
    
    @Query("SELECT w FROM WaitList w WHERE w.seat.id = ?1 AND w.status = 'WAITING' ORDER BY w.queuePosition ASC")
    List<WaitList> findWaitingListForSeat(Long seatId);
    
    @Query("SELECT COUNT(w) FROM WaitList w WHERE w.seat.id = ?1 AND w.status = 'WAITING'")
    int countWaitingForSeat(Long seatId);
    
    @Query("SELECT w FROM WaitList w WHERE w.user.id = ?1 AND w.seat.id = ?2 AND w.status = 'WAITING'")
    List<WaitList> findActiveWaitListItemByUserAndSeat(Long userId, Long seatId);
}