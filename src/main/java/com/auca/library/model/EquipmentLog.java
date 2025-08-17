package com.auca.library.model;

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
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "equipment_logs")
@Getter
@Setter
@NoArgsConstructor
public class EquipmentLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "equipment_id", nullable = false)
    private Equipment equipment;

    @Enumerated(EnumType.STRING)
    private EquipmentStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EquipmentStatus toStatus;

    @Column(nullable = false)
    private Integer quantity;

    @ManyToOne
    @JoinColumn(name = "changed_by", nullable = false)
    private User changedBy;

    @Column(nullable = false)
    private LocalDateTime changedAt;

    private String notes;

    @PrePersist
    protected void onCreate() {
        changedAt = LocalDateTime.now();
    }

    public EquipmentLog(Equipment equipment, EquipmentStatus fromStatus, EquipmentStatus toStatus, 
                       Integer quantity, User changedBy, String notes) {
        this.equipment = equipment;
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
        this.quantity = quantity;
        this.changedBy = changedBy;
        this.notes = notes;
    }
}