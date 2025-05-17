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

    // Flag to mark a day as completely closed
    @Column(name = "open", nullable = false)
    private boolean isOpen = true;

    // Special closing time for one-day modifications (e.g., early closing)
    @Column
    private LocalTime specialCloseTime;

    // Message explaining special arrangements
    @Column(columnDefinition = "TEXT")
    private String message;

    // Track when the schedule was last modified
    @Column
    private LocalDateTime lastModified;

    public LibrarySchedule(DayOfWeek dayOfWeek, LocalTime openTime, LocalTime closeTime) {
        this.dayOfWeek = dayOfWeek;
        this.openTime = openTime;
        this.closeTime = closeTime;
        this.isOpen = true;
        this.lastModified = LocalDateTime.now();
    }

    // Convenience method to get effective closing time (considers special closing)
    public LocalTime getEffectiveCloseTime() {
        return specialCloseTime != null ? specialCloseTime : closeTime;
    }

    // Method to check if library is open at a specific time
    public boolean isOpenAt(LocalTime time) {
        if (!isOpen) {
            return false;
        }
        return !time.isBefore(openTime) && time.isBefore(getEffectiveCloseTime());
    }
}