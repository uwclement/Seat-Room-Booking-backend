package com.auca.library.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(name = "equipment_requests")
@Getter
@Setter
@NoArgsConstructor
public class EquipmentRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "equipment_id", nullable = false)
    private Equipment equipment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id")
    private Course course; // Required for professors

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lab_class_id")
    private LabClass labClass; // If requesting lab class

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_booking_id")
    private RoomBooking roomBooking; // If part of room booking

    @Column(nullable = false)
    private String reason;

    @Column(nullable = false)
    private LocalDateTime startTime;

    @Column(nullable = false)
    private LocalDateTime endTime;

    @Column(nullable = false)
    private Integer requestedQuantity = 1;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RequestStatus status = RequestStatus.PENDING;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by")
    private User approvedBy;

    private LocalDateTime approvedAt;
    private String rejectionReason;
    private String adminSuggestion;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime updatedAt = LocalDateTime.now();

    // Escalation fields
    private boolean escalatedToHod = false;
    private LocalDateTime escalatedAt;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hod_reviewed_by")
    private User hodReviewedBy;
    private LocalDateTime hodReviewedAt;

    public enum RequestStatus {
        PENDING,
        APPROVED,
        REJECTED,
        ESCALATED,
        HOD_APPROVED,
        HOD_REJECTED,
        COMPLETED,
        CANCELLED
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}