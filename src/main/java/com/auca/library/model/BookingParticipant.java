package com.auca.library.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(name = "booking_participants")
@Getter
@Setter
@NoArgsConstructor
public class BookingParticipant {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false)
    private RoomBooking booking;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ParticipantStatus status = ParticipantStatus.INVITED;

    @Column(nullable = false)
    private boolean checkedIn = false;

    private LocalDateTime checkedInAt;

    private LocalDateTime invitedAt = LocalDateTime.now();
    private LocalDateTime respondedAt;

    private String invitationToken; // For secure invitation links

    public BookingParticipant(RoomBooking booking, User user) {
        this.booking = booking;
        this.user = user;
    }
}
