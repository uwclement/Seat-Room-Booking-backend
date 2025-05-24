package com.auca.library.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "rooms")
@Getter
@Setter
@NoArgsConstructor
public class Room {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String roomNumber;

    @Column(nullable = false)
    private String name;

    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RoomCategory category;

    @Column(nullable = false)
    private Integer capacity;

    @Column(nullable = false)
    private Integer maxBookingHours; // Maximum hours per booking session

    @Column(nullable = false)
    private Integer maxBookingsPerDay = 1; // Max bookings per user per day

    @Column(nullable = false)
    private Integer advanceBookingDays = 7; // How many days in advance users can book

    // For CLASS_ROOM category - admin can toggle availability
    @Column(nullable = false)
    private boolean available = true;

    // Room location details
    private String building;
    private String floor;
    private String department;

    // Equipment relationships
    @ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(name = "room_equipment",
            joinColumns = @JoinColumn(name = "room_id"),
            inverseJoinColumns = @JoinColumn(name = "equipment_id"))
    private Set<Equipment> equipment = new HashSet<>();

    // Maintenance and scheduling
    private LocalDateTime maintenanceStart;
    private LocalDateTime maintenanceEnd;
    private String maintenanceNotes;

    // Approval settings
    @Column(nullable = false)
    private boolean requiresApproval = false;

    // Timestamps
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    public Room(String roomNumber, String name, RoomCategory category, Integer capacity, Integer maxBookingHours) {
        this.roomNumber = roomNumber;
        this.name = name;
        this.category = category;
        this.capacity = capacity;
        this.maxBookingHours = maxBookingHours;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Helper methods
    public void addEquipment(Equipment equipment) {
        this.equipment.add(equipment);
    }

    public void removeEquipment(Equipment equipment) {
        this.equipment.remove(equipment);
    }

    public boolean isClassRoom() {
        return this.category == RoomCategory.CLASS_ROOM;
    }

    public boolean requiresBooking() {
        return this.category == RoomCategory.LIBRARY_ROOM || this.category == RoomCategory.STUDY_ROOM;
    }

    public boolean isUnderMaintenance() {
        if (maintenanceStart == null || maintenanceEnd == null) {
            return false;
        }
        LocalDateTime now = LocalDateTime.now();
        return now.isAfter(maintenanceStart) && now.isBefore(maintenanceEnd);
    }
}