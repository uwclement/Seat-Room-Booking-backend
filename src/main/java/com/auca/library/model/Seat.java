package com.auca.library.model;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Location location;

    // Zone type: collaboration or silent
    @Column(nullable = false)
    private String zoneType;

    @Column(nullable = false)
    private boolean hasDesktop;

    // For admin to disable seats during maintenance
    private boolean isDisabled = false;

    private String description;
    
     // QR CODE fields
    @Column(name = "qr_code_url")
    private String qrCodeUrl;

    @Column(name = "qr_code_token", unique = true)
    private String qrCodeToken;

    @Column(name = "qr_image_path", length = 500)
    private String qrImagePath;

    @Column(name = "qr_generated_at")
    private LocalDateTime qrGeneratedAt;

    @Column(name = "qr_version", nullable = false)
    private Integer qrVersion = 1;

    @OneToMany(mappedBy = "seat", cascade = CascadeType.ALL)
    private Set<Booking> bookings = new HashSet<>();

    @ManyToMany(mappedBy = "favoriteSeats")
    private Set<User> favoritedBy = new HashSet<>();

    public Seat(String seatNumber, String zoneType, boolean hasDesktop, String description, Location location) {
        this.seatNumber = seatNumber;
        this.zoneType = zoneType;
        this.hasDesktop = hasDesktop;
        this.description = description;
        this.location = location;
    }


    public boolean belongsToLocation(Location location) {
        return this.location.equals(location);
    }


    public String getLocationDisplayName() {
        return location != null ? location.getDisplayName() : "Unknown";
    }

     public String getLocationAwareSeatNumber() {
        if (location != null) {
            return location.getCode() + "-" + seatNumber;
        }
        return seatNumber;
    }
    
}