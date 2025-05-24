package com.auca.library.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "room_templates")
@Getter
@Setter
@NoArgsConstructor
public class RoomTemplate {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String templateName;

    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RoomCategory category;

    @Column(nullable = false)
    private Integer capacity;

    @Column(nullable = false)
    private Integer maxBookingHours;

    @Column(nullable = false)
    private Integer maxBookingsPerDay = 1;

    @Column(nullable = false)
    private Integer advanceBookingDays = 7;

    @Column(nullable = false)
    private boolean requiresApproval = false;

    // Default equipment for this template
    @ManyToMany
    @JoinTable(name = "template_equipment",
            joinColumns = @JoinColumn(name = "template_id"),
            inverseJoinColumns = @JoinColumn(name = "equipment_id"))
    private Set<Equipment> defaultEquipment = new HashSet<>();

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public RoomTemplate(String templateName, RoomCategory category, Integer capacity, Integer maxBookingHours) {
        this.templateName = templateName;
        this.category = category;
        this.capacity = capacity;
        this.maxBookingHours = maxBookingHours;
    }
}