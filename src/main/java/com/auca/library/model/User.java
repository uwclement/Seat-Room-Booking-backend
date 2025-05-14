package com.auca.library.model;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.auca.library.converter.NotificationListConverter;
import com.auca.library.dto.request.NotificationMessage;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "users", 
       uniqueConstraints = {
           @UniqueConstraint(columnNames = "email"),
           @UniqueConstraint(columnNames = "studentId")
       })
@Getter
@Setter
@NoArgsConstructor
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(max = 100)
    private String fullName;

    @NotBlank
    @Size(max = 50)
    @Email
    private String email;

    @NotBlank
    @Size(max = 20)
    private String studentId;

    @NotBlank
    @Size(max = 120)
    private String password;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "user_roles", 
               joinColumns = @JoinColumn(name = "user_id"),
               inverseJoinColumns = @JoinColumn(name = "role_id"))
    private Set<Role> roles = new HashSet<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "user_favorite_seats",
           joinColumns = @JoinColumn(name = "user_id"),
           inverseJoinColumns = @JoinColumn(name = "seat_id"))
     private Set<Seat> favoriteSeats = new HashSet<>();

    @Column(nullable = false)
    private boolean emailVerified = false;
    
    private String verificationToken;

    public User(String fullName, String email, String studentId, String password) {
        this.fullName = fullName;
        this.email = email;
        this.studentId = studentId;
        this.password = password;
    }

@Column(name = "recent_notifications", columnDefinition = "jsonb")
    @Convert(converter = NotificationListConverter.class)
    private List<NotificationMessage> recentNotifications = new ArrayList<>();
    
    // Methods for backward compatibility
    public List<NotificationMessage> getNotifications() {
        return this.recentNotifications;
    }
    
    public void setNotificationsList(List<NotificationMessage> notifications) {
        this.recentNotifications = notifications;
    }
    
    @Deprecated
    public void saveNotifications() {
        // No longer needed - kept for compatibility
    }
    
}