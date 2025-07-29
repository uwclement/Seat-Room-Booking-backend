package com.auca.library.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.DayOfWeek;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.CollectionTable;

@Entity
@Table(name = "users", 
       uniqueConstraints = {
           @UniqueConstraint(columnNames = "email"),
           @UniqueConstraint(columnNames = "studentId"),
           @UniqueConstraint(columnNames = "employeeId")
       })
@Getter
@Setter
@NoArgsConstructor
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Common fields for all users
    @NotBlank
    @Size(max = 100)
    private String fullName;

    @NotBlank
    @Size(max = 50)
    @Email
    private String email;

    @NotBlank
    @Size(max = 120)
    private String password;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Location location;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "user_roles", 
               joinColumns = @JoinColumn(name = "user_id"),
               inverseJoinColumns = @JoinColumn(name = "role_id"))
    private Set<Role> roles = new HashSet<>();

    // Authentication and verification fields
    @Column(nullable = false)
    private boolean emailVerified = false;
    
    private String verificationToken;
    
    @Column(nullable = false)
    private boolean mustChangePassword = false;

    // Student based fields nullable
    @Size(max = 20)
    private String studentId;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "user_favorite_seats",
           joinColumns = @JoinColumn(name = "user_id"),
           inverseJoinColumns = @JoinColumn(name = "seat_id"))
    private Set<Seat> favoriteSeats = new HashSet<>();

    // staff based fileds nullable
    @Size(max = 20)
    private String employeeId;

    @Size(max = 15)
    private String phone;

    // Librarian based fileds
    @ElementCollection
    @Enumerated(EnumType.STRING)
    @CollectionTable(name = "user_working_days", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "day_of_week")
    private Set<DayOfWeek> workingDays = new HashSet<>();
    @Column(name = "active_this_week")
    private boolean activeThisWeek = false;   
    private boolean activeToday = false;
    @Column(name = "is_default_librarian")
    private boolean isDefaultLibrarian = false;


    // Professor based fields
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "professor_courses",
        joinColumns = @JoinColumn(name = "user_id"),
        inverseJoinColumns = @JoinColumn(name = "course_id"))
    private Set<Course> approvedCourses = new HashSet<>();

    private boolean professorApproved = false;
    private LocalDateTime professorApprovedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by_hod")
    private User approvedByHod;

    // Constructors
    public User(String fullName, String email, String password, Location location) {
        this.fullName = fullName;
        this.email = email;
        this.password = password;
        this.location = location;
    }

    // Student constructor
    public User(String fullName, String email, String studentId, String password, Location location) {
        this(fullName, email, password, location);
        this.studentId = studentId;
    }

    // Staff constructor
    public User(String fullName, String email, String employeeId, String password, Location location, String phone) {
        this(fullName, email, password, location);
        this.employeeId = employeeId;
        this.phone = phone;
        this.mustChangePassword = true; // Staff must change default password
        this.emailVerified = true; // Staff accounts are pre-verified
    }

    // Utility methods
    public boolean belongsToLocation(Location location) {
        return this.location != null && this.location.equals(location);
    }
    
    public String getLocationDisplayName() {
        return location != null ? location.getDisplayName() : "Unknown";
    }

    public boolean isStudent() {
        return studentId != null && !studentId.isEmpty();
    }

    public boolean isStaff() {
        return employeeId != null && !employeeId.isEmpty();
    }

    public String getIdentifier() {
        return isStudent() ? studentId : employeeId;
    }

    // Role checking methods
    public boolean hasRole(Role.ERole roleName) {
        return roles.stream()
                .anyMatch(role -> role.getName().equals(roleName));
    }

    public boolean isLibrarian() {
        return hasRole(Role.ERole.ROLE_LIBRARIAN);
    }

    public boolean isAdmin() {
        return hasRole(Role.ERole.ROLE_ADMIN);
    }

    public boolean isProfessor() {
        return hasRole(Role.ERole.ROLE_PROFESSOR);
    }

    public boolean isHod() {
        return hasRole(Role.ERole.ROLE_HOD);
    }

    public boolean isEquipmentAdmin() {
        return hasRole(Role.ERole.ROLE_EQUIPMENT_ADMIN);
    }

    public boolean isActiveLibrarianToday() {
    DayOfWeek today = LocalDate.now().getDayOfWeek();
    return isLibrarian() && activeThisWeek && workingDays.contains(today);
}

// Librarian menthods 

public boolean worksOnDay(DayOfWeek day) {
    return workingDays.contains(day);
}

public String getWorkingDaysString() {
    return workingDays.stream()
            .map(DayOfWeek::toString)
            .collect(Collectors.joining(", "));
}

public void addWorkingDay(DayOfWeek day) {
    this.workingDays.add(day);
}

public void removeWorkingDay(DayOfWeek day) {
    this.workingDays.remove(day);
}

public void setWorkingDays(Set<DayOfWeek> workingDays) {
    this.workingDays = workingDays != null ? workingDays : new HashSet<>();
}
}