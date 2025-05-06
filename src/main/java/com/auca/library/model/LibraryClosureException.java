package com.auca.library.model;

import java.time.LocalDate;
import java.time.LocalTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "library_closure_exceptions")
@Getter
@Setter
@NoArgsConstructor
public class LibraryClosureException {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDate date;

    private LocalTime openTime;

    private LocalTime closeTime;

    private boolean closedAllDay = false;

    private String reason;

    public LibraryClosureException(LocalDate date, boolean closedAllDay, String reason) {
        this.date = date;
        this.closedAllDay = closedAllDay;
        this.reason = reason;
    }

    public LibraryClosureException(LocalDate date, LocalTime openTime, LocalTime closeTime, String reason) {
        this.date = date;
        this.openTime = openTime;
        this.closeTime = closeTime;
        this.closedAllDay = false;
        this.reason = reason;
    }
}