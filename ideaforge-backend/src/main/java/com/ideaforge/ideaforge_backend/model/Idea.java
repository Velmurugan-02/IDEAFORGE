package com.ideaforge.ideaforge_backend.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Represents a startup idea submitted by a user.
 * Central entity of the IdeaForge platform – linked to votes, comments, battles, etc.
 */
@Entity
@Table(name = "ideas")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Idea {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    /** Short elevator-pitch text describing the idea. */
    @Column(columnDefinition = "TEXT")
    private String pitch;

    /** Detailed description of the problem the idea solves. */
    @Column(columnDefinition = "TEXT")
    private String problem;

    @Column(name = "target_audience", length = 100)
    private String targetAudience;

    /** Broad category tag (e.g. FinTech, EdTech, HealthTech). */
    @Column(length = 50)
    private String category;

    /** Owner / creator of this idea. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** Computed traction score updated by the platform based on votes / comments / battles. */
    @Column(name = "traction_score", nullable = false)
    @Builder.Default
    private Double tractionScore = 0.0;

    @Column(name = "vote_count", nullable = false)
    @Builder.Default
    private Integer voteCount = 0;

    @Column(name = "comment_count", nullable = false)
    @Builder.Default
    private Integer commentCount = 0;

    /** Lifecycle status within the Idea Battle feature. */
    @Enumerated(EnumType.STRING)
    @Column(name = "battle_status", nullable = false)
    @Builder.Default
    private BattleStatus battleStatus = BattleStatus.NONE;

    /** Controls who can see this idea. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private Visibility visibility = Visibility.PUBLIC;

    /** Whether the user has agreed to the IP-protection terms for this idea. */
    @Column(name = "agreed_to_terms", nullable = false)
    @Builder.Default
    private Boolean agreedToTerms = false;

    /** Timestamp at which the user accepted the terms (null until accepted). */
    @Column(name = "terms_agreed_at")
    private LocalDateTime termsAgreedAt;

    /** Flagged true if a plagiarism report against this idea has been confirmed. */
    @Column(name = "is_plagiarised", nullable = false)
    @Builder.Default
    private Boolean isPlagiarised = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    // ------------------------------------------------------------------
    // Nested enums
    // ------------------------------------------------------------------

    public enum BattleStatus { NONE, ACTIVE, WON, LOST }

    public enum Visibility { PUBLIC, PRIVATE, INVITE_ONLY }
}
