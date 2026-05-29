package com.ideaforge.ideaforge_backend.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Records each unique daily view of an idea.
 * Used as civil evidence that an idea was viewed before any real-world implementation.
 * One entry per (idea, viewer, day) — updated on subsequent views that day.
 */
@Entity
@Table(name = "view_logs", indexes = {
        @Index(name = "idx_vl_idea_viewer", columnList = "idea_id, viewer_id"),
        @Index(name = "idx_vl_idea_id",     columnList = "idea_id")
})
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class ViewLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The idea that was viewed. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "idea_id", nullable = false)
    private Idea idea;

    /** The authenticated user who viewed the idea. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "viewer_id", nullable = false)
    private User viewer;

    /** Raw or X-Forwarded-For IP address (up to IPv6 length = 45 chars). */
    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    /** SHA-256 of the User-Agent string — used as a device fingerprint. */
    @Column(name = "device_fingerprint", length = 64)
    private String deviceFingerprint;

    /** First/last view timestamp for the day. */
    @Column(name = "viewed_at", nullable = false)
    @Builder.Default
    private LocalDateTime viewedAt = LocalDateTime.now();

    /**
     * Whether the viewer has explicitly clicked "I acknowledge this idea is protected"
     * (i.e. they gave informed consent that viewing creates an evidence trail).
     */
    @Column(name = "consent_given", nullable = false)
    @Builder.Default
    private Boolean consentGiven = false;

    /** Timestamp at which consent was given; null until consented. */
    @Column(name = "consent_given_at")
    private LocalDateTime consentGivenAt;
}
