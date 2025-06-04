package com.auca.library.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "room_bookings")
@Getter
@Setter
@NoArgsConstructor
public class RoomBooking {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user; // Primary booker

    @Column(nullable = false)
    private LocalDateTime startTime;

    @Column(nullable = false)
    private LocalDateTime endTime;

    private boolean available;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BookingStatus status = BookingStatus.PENDING;

    @Column(nullable = false)
    private String title;

    private String description;

    // Sharing and collaboration
    @OneToMany(mappedBy = "booking", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<BookingParticipant> participants = new HashSet<>();

    @Column(nullable = false)
    private Integer maxParticipants;

    @Column(nullable = false)
    private boolean isPublic = false; // For joinable bookings

    @Column(nullable = false)
    private boolean allowJoining = false;

    // Recurring booking
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recurring_series_id")
    private RecurringBookingSeries recurringBookingSeries;

    // Check-in tracking
    @Column(nullable = false)
    private boolean requiresCheckIn = true;

    private LocalDateTime checkedInAt;

    @Column(nullable = false)
    private boolean autoCheckInEnabled = false;

    // Equipment requests
    @ManyToMany
    @JoinTable(name = "booking_equipment_requests",
            joinColumns = @JoinColumn(name = "booking_id"),
            inverseJoinColumns = @JoinColumn(name = "equipment_id"))
    private Set<Equipment> requestedEquipment = new HashSet<>();

    // Admin controls
    @Column(nullable = false)
    private boolean requiresApproval = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by")
    private User approvedBy;

    private LocalDateTime approvedAt;

    private String rejectionReason;

    // Timestamps
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    // Notifications
    private LocalDateTime reminderSentAt;

    @Column(nullable = false)
    private boolean reminderEnabled = true;

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Helper methods
    public boolean isActive() {
        return status == BookingStatus.CONFIRMED && 
               LocalDateTime.now().isBefore(endTime);
    }

    public boolean canCheckIn() {
        LocalDateTime now = LocalDateTime.now();
        return status == BookingStatus.CONFIRMED && 
               now.isAfter(startTime.minusMinutes(15)) && 
               now.isBefore(endTime) && 
               checkedInAt == null;
    }

    public boolean isOverdue() {
        return status == BookingStatus.CONFIRMED && 
               checkedInAt == null && 
               LocalDateTime.now().isAfter(startTime.plusMinutes(20));
    }

    public int getCheckedInCount() {
        return (int) participants.stream()
                .filter(p -> p.getCheckedInAt() != null)
                .count() + (checkedInAt != null ? 1 : 0);
    }

    public enum BookingStatus {
        PENDING,        // Waiting for approval
        CONFIRMED,      // Approved and scheduled
        CHECKED_IN,     // User has checked in
        COMPLETED,      // Booking finished
        CANCELLED,      // Cancelled by user/admin
        NO_SHOW,        // Auto-cancelled due to no check-in
        REJECTED        // Rejected by admin
    }
}
