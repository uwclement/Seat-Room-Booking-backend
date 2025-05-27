package com.auca.library.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.DayOfWeek;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "recurring_bookings")
@Getter
@Setter
@NoArgsConstructor
public class RecurringBooking {
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
    private String title;

    @Column(nullable = false)
    private LocalTime startTime;

    @Column(nullable = false)
    private LocalTime endTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DayOfWeek dayOfWeek;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RecurrencePattern pattern = RecurrencePattern.WEEKLY;

    @Column(nullable = false)
    private LocalDateTime startDate;

    @Column(nullable = false)
    private LocalDateTime endDate;

    @Column(nullable = false)
    private boolean active = true;

    // Individual bookings created from this recurring pattern
    @OneToMany(mappedBy = "recurringBooking", cascade = CascadeType.ALL)
    private Set<RoomBooking> generatedBookings = new HashSet<>();

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public RecurringBooking(Room room, User user, String title, LocalTime startTime, 
                           LocalTime endTime, DayOfWeek dayOfWeek, LocalDateTime startDate, 
                           LocalDateTime endDate) {
        this.room = room;
        this.user = user;
        this.title = title;
        this.startTime = startTime;
        this.endTime = endTime;
        this.dayOfWeek = dayOfWeek;
        this.startDate = startDate;
        this.endDate = endDate;
    }
}