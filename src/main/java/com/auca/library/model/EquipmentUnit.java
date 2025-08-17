package com.auca.library.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "equipment_units")
@Getter
@Setter
@NoArgsConstructor
public class EquipmentUnit {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "equipment_id", nullable = false)
    private Equipment equipment;

    @Column(nullable = false, unique = true)
    private String serialNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UnitStatus status = UnitStatus.AVAILABLE;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    // Additional tracking fields
    private String condition = "GOOD"; // GOOD, FAIR, POOR
    private LocalDate purchaseDate;
    private LocalDate warrantyExpiry;
    private String notes;

    public enum UnitStatus {
        AVAILABLE,
        ASSIGNED,
        IN_REQUEST,
        MAINTENANCE,
        DAMAGED,
        LOST
    }

    public EquipmentUnit(Equipment equipment, String serialNumber) {
        this.equipment = equipment;
        this.serialNumber = serialNumber;
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Helper methods
    public boolean isAvailable() {
        return status == UnitStatus.AVAILABLE;
    }

    public boolean isAssigned() {
        return status == UnitStatus.ASSIGNED || status == UnitStatus.IN_REQUEST;
    }

    public String getEquipmentName() {
        return equipment != null ? equipment.getName() : "";
    }

    public Location getLocation() {
        return equipment != null ? equipment.getLocation() : null;
    }
}