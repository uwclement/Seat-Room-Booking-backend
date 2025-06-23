package com.auca.library.model;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "librarian")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Librarian {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String phone;

    private boolean activeToday;

    private boolean isDefault;

    private LocalDate workingDay;

    public boolean isDefault() {
    return isDefault;
 }

    public void setIsDefault(boolean isDefault) {
    this.isDefault = isDefault;
 }

}
