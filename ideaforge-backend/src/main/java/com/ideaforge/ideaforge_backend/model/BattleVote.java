package com.ideaforge.ideaforge_backend.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Records a user's vote in a specific battle.
 * Enforces one vote per user per battle.
 */
@Entity
@Table(
    name = "battle_votes",
    uniqueConstraints = @UniqueConstraint(name = "one_vote_per_battle", columnNames = {"battle_id", "user_id"})
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BattleVote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "battle_id", nullable = false)
    private Battle battle;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "voted_idea_id", nullable = false)
    private Long votedIdeaId;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
