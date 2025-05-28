package com.auca.library.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.DayOfWeek;
import java.util.Set;
import java.util.HashSet;

@Entity
@Table(name = "recurring_booking_series")
@Getter
@Setter
@NoArgsConstructor
public class RecurringBookingSeries {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @Column(nullable = false)
    private String title;

    private String description;

    // Recurrence pattern
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RecurrenceType recurrenceType = RecurrenceType.WEEKLY;

    @Column(nullable = false)
    private Integer recurrenceInterval = 1; // Every X weeks/days

    @ElementCollection(targetClass = DayOfWeek.class)
    @Enumerated(EnumType.STRING)
    @CollectionTable(name = "recurring_booking_days")
    private Set<DayOfWeek> daysOfWeek = new HashSet<>();

    @Column(nullable = false)
    private LocalTime startTime;

    @Column(nullable = false)
    private LocalTime endTime;

    @Column(nullable = false)
    private LocalDateTime seriesStartDate;

    private LocalDateTime seriesEndDate;

    @Column(nullable = false)
    private boolean isActive = true;

    @OneToMany(mappedBy = "recurringBookingSeries", cascade = CascadeType.ALL)
    private Set<RoomBooking> bookings = new HashSet<>();

    private LocalDateTime lastGeneratedDate;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public enum RecurrenceType {
        DAILY,
        WEEKLY,
        MONTHLY,
        CUSTOM
    }
}