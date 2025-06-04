package com.auca.library.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.auca.library.model.BookingParticipant;
import com.auca.library.model.RoomBooking;
import com.auca.library.model.User;

@Repository
public interface BookingParticipantRepository extends JpaRepository<BookingParticipant, Long> {
    
    List<BookingParticipant> findByBookingAndStatus(RoomBooking booking, BookingParticipant.ParticipantStatus status);
    
    List<BookingParticipant> findByUserAndStatusIn(User user, List<BookingParticipant.ParticipantStatus> statuses);
    
    Optional<BookingParticipant> findByBookingAndUser(RoomBooking booking, User user);
    
    @Query("SELECT COUNT(bp) FROM BookingParticipant bp WHERE bp.booking = :booking AND bp.status = 'ACCEPTED'")
    Long countAcceptedParticipants(@Param("booking") RoomBooking booking);
    
    @Query("SELECT COUNT(bp) FROM BookingParticipant bp WHERE bp.booking = :booking AND bp.checkedInAt IS NOT NULL")
    Long countCheckedInParticipants(@Param("booking") RoomBooking booking);
    
    @Query("SELECT bp FROM BookingParticipant bp WHERE bp.status = 'INVITED' AND " +
           "bp.notificationSent = false")
    List<BookingParticipant> findPendingInvitationNotifications();
}

