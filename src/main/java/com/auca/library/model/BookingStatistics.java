package com.auca.library.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "booking_statistics")
@Getter
@Setter
@NoArgsConstructor
public class BookingStatistics {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private LocalDate date;
    
    @Column(nullable = false)
    private int totalBookings;
    
    @Column(nullable = false)
    private int uniqueUsers;
    
    @Column(nullable = false)
    private double averageBookingDuration; // in minutes
    
    @Column(nullable = false)
    private double peakOccupancyRate; // percentage
    
    private int bookingsInSilentZone;
    
    private int bookingsInCollaborationZone;
    
    private LocalDateTime peakBookingHour;
    
    private int peakHourBookings;
    
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}