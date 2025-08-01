package com.auca.library.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "roles")
@Getter
@Setter
@NoArgsConstructor
public class Role {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private ERole name;

    public Role(ERole name) {
        this.name = name;
    }

    public enum ERole {
        ROLE_USER,  
        ROLE_ADMIN,
        ROLE_EQUIPMENT_ADMIN, 
        ROLE_PROFESSOR,      
        ROLE_HOD, 
        ROLE_LIBRARIAN
    }
}