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
    private User user; // Main booker

    @Column(nullable = false)
    private LocalDateTime startTime;

    @Column(nullable = false)
    private LocalDateTime endTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BookingStatus status = BookingStatus.PENDING;

    @Column(nullable = false)
    private boolean isRecurring = false;

    // Recurring booking reference
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recurring_booking_id")
    private RecurringBooking recurringBooking;

    // Sharing and collaboration
    @Column(nullable = false)
    private boolean isShared = false;

    @Column(nullable = false)
    private boolean isJoinable = false; // Admin can make bookings joinable

    @Column(nullable = false)
    private Integer maxParticipants;

    // Check-in tracking
    @Column(nullable = false)
    private boolean checkedIn = false;

    private LocalDateTime checkedInAt;

    @Column(nullable = false)
    private boolean checkInRequired = true;

    // Approval workflow
    @Column(nullable = false)
    private boolean requiresApproval = false;

    private String approvalNotes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by")
    private User approvedBy;

    private LocalDateTime approvedAt;

    // Booking details
    private String purpose;
    private String notes;

    // Equipment requests
    @ManyToMany
    @JoinTable(name = "booking_equipment",
            joinColumns = @JoinColumn(name = "booking_id"),
            inverseJoinColumns = @JoinColumn(name = "equipment_id"))
    private Set<Equipment> requestedEquipment = new HashSet<>();

    // Participants
    @OneToMany(mappedBy = "booking", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<BookingParticipant> participants = new HashSet<>();

    // Timestamps
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Helper methods
    public boolean isActive() {
        return status == BookingStatus.CONFIRMED && 
               LocalDateTime.now().isBefore(endTime) && 
               LocalDateTime.now().isAfter(startTime);
    }

    public boolean isUpcoming() {
        return status == BookingStatus.CONFIRMED && 
               LocalDateTime.now().isBefore(startTime);
    }

    public boolean isPast() {
        return LocalDateTime.now().isAfter(endTime);
    }

    public boolean canCheckIn() {
        LocalDateTime now = LocalDateTime.now();
        return status == BookingStatus.CONFIRMED && 
               now.isAfter(startTime.minusMinutes(15)) && 
               now.isBefore(endTime) && 
               !checkedIn;
    }

    public boolean isOverdue() {
        return checkInRequired && !checkedIn && 
               LocalDateTime.now().isAfter(startTime.plusMinutes(20));
    }

    public int getCurrentParticipantCount() {
        return (int) participants.stream()
                .filter(p -> p.getStatus() == ParticipantStatus.ACCEPTED)
                .count() + 1; // +1 for the main booker
    }

    public boolean hasAvailableSlots() {
        return getCurrentParticipantCount() < maxParticipants;
    }
}
