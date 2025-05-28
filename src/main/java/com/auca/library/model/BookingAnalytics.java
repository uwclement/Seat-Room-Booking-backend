package com.auca.library.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "booking_analytics")
@Getter
@Setter
@NoArgsConstructor
public class BookingAnalytics {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private LocalDate date;

    @Column(nullable = false)
    private Integer bookingDurationMinutes;

    @Column(nullable = false)
    private Integer actualUsageDurationMinutes = 0;

    @Column(nullable = false)
    private Integer participantCount = 1;

    @Column(nullable = false)
    private boolean checkedIn = false;

    @Column(nullable = false)
    private boolean wasNoShow = false;

    @Column(nullable = false)
    private boolean wasCancelled = false;

    @Enumerated(EnumType.STRING)
    private RoomBooking.BookingStatus finalStatus;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}