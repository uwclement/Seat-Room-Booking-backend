package com.auca.library.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "courses")
@Getter
@Setter
@NoArgsConstructor
public class Course {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(unique = true, length = 20)
    private String courseCode;

    @NotBlank
    @Column(length = 100)
    private String courseName;

    @Positive
    private Integer creditHours;

    @Column(nullable = false)
    private boolean active = true;

    // Many-to-many relationship with professors
    @ManyToMany(mappedBy = "approvedCourses")
    private Set<User> professors = new HashSet<>();

    public Course(String courseCode, String courseName, Integer creditHours) {
        this.courseCode = courseCode;
        this.courseName = courseName;
        this.creditHours = creditHours;
    }
}