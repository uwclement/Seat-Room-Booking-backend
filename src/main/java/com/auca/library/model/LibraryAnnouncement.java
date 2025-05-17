package com.auca.library.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "library_announcements")
@Getter
@Setter
@NoArgsConstructor
public class LibraryAnnouncement {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String title;
    
    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;
    
    @Column(nullable = false)
    private LocalDateTime startDateTime;
    
    @Column(nullable = false)
    private LocalDateTime endDateTime;
    
    // Whether this announcement should be visible in the UI
    @Column(nullable = false)
    private boolean isUIVisible = true;
    
    // Whether this announcement should trigger notifications
    @Column(nullable = false)
    private boolean isNotificationEnabled = false;
    
    // Admin who created this announcement
    @ManyToOne
    @JoinColumn(name = "created_by", referencedColumnName = "id")
    private User createdBy;
    
    @Column(nullable = false)
    private LocalDateTime createdAt;
    
    @Column
    private LocalDateTime updatedAt;
    
    public LibraryAnnouncement(String title, String message, LocalDateTime startDateTime, 
                              LocalDateTime endDateTime, boolean isUIVisible, 
                              boolean isNotificationEnabled, User createdBy) {
        this.title = title;
        this.message = message;
        this.startDateTime = startDateTime;
        this.endDateTime = endDateTime;
        this.isUIVisible = isUIVisible;
        this.isNotificationEnabled = isNotificationEnabled;
        this.createdBy = createdBy;
        this.createdAt = LocalDateTime.now();
    }
    
    // Check if announcement is currently active
    public boolean isActive() {
        LocalDateTime now = LocalDateTime.now();
        return (now.isAfter(startDateTime) || now.isEqual(startDateTime)) && 
               now.isBefore(endDateTime);
    }
}