package com.auca.library.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "wait_lists")
@Getter
@Setter
@NoArgsConstructor
public class WaitList {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne
    @JoinColumn(name = "seat_id", nullable = false)
    private Seat seat;

    @Column(nullable = false)
    private LocalDateTime requestedStartTime;

    @Column(nullable = false)
    private LocalDateTime requestedEndTime;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    // Position in the queue for this seat
    private int queuePosition;

    // Whether the user has been notified of availability
    private boolean notified = false;

    // When the user was notified
    private LocalDateTime notifiedAt;

    // Current status of the wait list item
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WaitListStatus status = WaitListStatus.WAITING;

    public enum WaitListStatus {
        WAITING,        // User is waiting for the seat
        NOTIFIED,       // User was notified of availability
        FULFILLED,      // User got the seat
        EXPIRED,        // Notification expired without response
        CANCELLED       // User cancelled the waitlist request
    }

    public WaitList(User user, Seat seat, LocalDateTime requestedStartTime, LocalDateTime requestedEndTime) {
        this.user = user;
        this.seat = seat;
        this.requestedStartTime = requestedStartTime;
        this.requestedEndTime = requestedEndTime;
    }
}