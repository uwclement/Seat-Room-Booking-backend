package com.auca.library.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(name = "lab_class_requests")
@Getter
@Setter
@NoArgsConstructor
public class LabRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lab_class_id", nullable = false)
    private LabClass labClass;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @Column(nullable = false)
    private String reason;

    @Column(nullable = false)
    private LocalDateTime startTime;

    @Column(nullable = false)
    private LocalDateTime endTime;

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
}