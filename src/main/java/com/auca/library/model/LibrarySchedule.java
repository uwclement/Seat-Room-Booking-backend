package com.auca.library.model;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "library_schedules")
@Getter
@Setter
@NoArgsConstructor
public class LibrarySchedule {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DayOfWeek dayOfWeek;

    @Column(nullable = false)
    private LocalTime openTime;

    @Column(nullable = false)
    private LocalTime closeTime;

    // Add location field
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Location location;

    @Column(name = "open", nullable = false)
    private boolean isOpen = true;

    @Column
    private LocalTime specialCloseTime;

    @Column(columnDefinition = "TEXT")
    private String message;

    @Column
    private LocalDateTime lastModified;

    public LibrarySchedule(DayOfWeek dayOfWeek, LocalTime openTime, LocalTime closeTime, Location location) {
        this.dayOfWeek = dayOfWeek;
        this.openTime = openTime;
        this.closeTime = closeTime;
        this.location = location;
        this.isOpen = true;
        this.lastModified = LocalDateTime.now();
    }

    public LocalTime getEffectiveCloseTime() {
        return specialCloseTime != null ? specialCloseTime : closeTime;
    }

    public boolean isOpenAt(LocalTime time) {
        if (!isOpen) {
            return false;
        }
        return !time.isBefore(openTime) && time.isBefore(getEffectiveCloseTime());
    }
}