package com.auca.library.model;

import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "lab_classes")
@Getter
@Setter
@NoArgsConstructor
public class LabClass {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(unique = true, length = 50)
    private String labNumber;

    @NotBlank
    @Column(length = 100)
    private String name;

    private String description;

    @Positive
    private Integer capacity;

    @Column(nullable = false)
    private String building;

    @Column(nullable = false)
    private String floor;

    @Column(nullable = false)
    private boolean available = true;
 
    @OneToMany(mappedBy = "labClass", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<LabRequest> labRequests = new HashSet<>();

    @OneToMany(mappedBy = "labClass", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<EquipmentRequest> equipmentRequests = new HashSet<>();

    // Equipment available in this lab
    @ManyToMany
    @JoinTable(
        name = "lab_equipment",
        joinColumns = @JoinColumn(name = "lab_id"),
        inverseJoinColumns = @JoinColumn(name = "equipment_id")
    )
    private Set<Equipment> equipment = new HashSet<>();

    public LabClass(String labNumber, String name, Integer capacity, String building, String floor) {
        this.labNumber = labNumber;
        this.name = name;
        this.capacity = capacity;
        this.building = building;
        this.floor = floor;
    }
}