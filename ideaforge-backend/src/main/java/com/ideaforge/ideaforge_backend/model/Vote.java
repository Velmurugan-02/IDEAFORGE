package com.ideaforge.ideaforge_backend.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Records a single up/down vote cast by a user on an idea.
 * Unique constraint (idea_id, user_id) enforces one-vote-per-user-per-idea.
 */
@Entity
@Table(
    name = "votes",
    uniqueConstraints = @UniqueConstraint(name = "one_vote_per_idea", columnNames = {"idea_id", "user_id"})
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Vote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "idea_id", nullable = false)
    private Idea idea;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VoteType type;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    // ------------------------------------------------------------------
    // Nested enum
    // ------------------------------------------------------------------

    public enum VoteType { UP, DOWN }
}
