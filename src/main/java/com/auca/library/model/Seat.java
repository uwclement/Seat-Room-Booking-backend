package com.auca.library.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "seats")
@Getter
@Setter
@NoArgsConstructor
public class Seat {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String seatNumber;

    // Zone type: collaboration or silent
    @Column(nullable = false)
    private String zoneType;

    @Column(nullable = false)
    private boolean hasDesktop;

    // For admin to disable seats during maintenance
    private boolean isDisabled = false;

    private String description;

    @OneToMany(mappedBy = "seat", cascade = CascadeType.ALL)
    private Set<Booking> bookings = new HashSet<>();

    @ManyToMany(mappedBy = "favoriteSeats")
    private Set<User> favoritedBy = new HashSet<>();

    public Seat(String seatNumber, String zoneType, boolean hasDesktop, String description) {
        this.seatNumber = seatNumber;
        this.zoneType = zoneType;
        this.hasDesktop = hasDesktop;
        this.description = description;
    }
}