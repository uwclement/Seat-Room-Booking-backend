package com.auca.library.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "qr_code_logs")
@Getter
@Setter
@NoArgsConstructor
public class QRCodeLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "resource_type", nullable = false, length = 50)
    private String resourceType; // SEAT or ROOM

    @Column(name = "resource_id", nullable = false)
    private Long resourceId;

    @Column(name = "qr_version", nullable = false)
    private Integer qrVersion = 1;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "generated_by", nullable = false)
    private User generatedBy;

    @Column(name = "generated_at", nullable = false)
    private LocalDateTime generatedAt = LocalDateTime.now();

    @Column(name = "is_current", nullable = false)
    private boolean isCurrent = true;

    @Column(name = "old_token")
    private String oldToken;

    @Column(name = "new_token", nullable = false)
    private String newToken;

    @Column(name = "generation_reason")
    private String generationReason;

    public QRCodeLog(String resourceType, Long resourceId, User generatedBy, String newToken) {
        this.resourceType = resourceType;
        this.resourceId = resourceId;
        this.generatedBy = generatedBy;
        this.newToken = newToken;
    }
}