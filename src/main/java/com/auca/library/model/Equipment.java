package com.auca.library.model;

import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "equipment")
@Getter
@Setter
@NoArgsConstructor
public class Equipment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    private String description;

    @Column(nullable = false)
    private boolean available = true;

    // Allow students to request this equipment
    @Column(nullable = false)
    private boolean allowedToStudents = false;

    private Integer quantity;

    private Integer availableQuantity;


    @ManyToMany(mappedBy = "equipment")
    private Set<Room> rooms = new HashSet<>();

    // Lab classes that have this equipment
    @ManyToMany(mappedBy = "equipment")
    private Set<LabClass> labClasses = new HashSet<>();

    public Equipment(String name, String description) {
        this.name = name;
        this.description = description;
        this.quantity = 1; // efault quantity
        this.availableQuantity = 1;
    }

    // Helper method to check if equipment is available in requested quantity
    public boolean isAvailableInQuantity(int requestedQuantity) {
        return available && availableQuantity != null && availableQuantity >= requestedQuantity;
    }
}