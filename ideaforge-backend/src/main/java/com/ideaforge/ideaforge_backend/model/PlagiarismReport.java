package com.ideaforge.ideaforge_backend.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * A plagiarism report filed by a community member claiming that one idea
 * is derived from or copies another. Reviewed by platform moderators.
 */
@Entity
@Table(name = "plagiarism_reports")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlagiarismReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The idea that has been flagged as potentially plagiarised. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reported_idea_id", nullable = false)
    private Idea reportedIdea;

    /** The original idea that the reported idea allegedly copies. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "original_idea_id", nullable = false)
    private Idea originalIdea;

    /** The user who filed this report. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reported_by", nullable = false)
    private User reportedBy;

    /** Free-text reason / evidence provided by the reporter. */
    @Column(columnDefinition = "TEXT")
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ReportStatus status = ReportStatus.PENDING;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    // ------------------------------------------------------------------
    // Nested enum
    // ------------------------------------------------------------------

    public enum ReportStatus { PENDING, CONFIRMED, DISMISSED }
}
