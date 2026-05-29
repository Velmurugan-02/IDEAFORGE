package com.ideaforge.ideaforge_backend.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * An Idea Battle – a head-to-head competition between two ideas where the
 * community votes to decide the winner within a time window.
 */
@Entity
@Table(name = "battles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Battle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The first competing idea. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "idea_a_id", nullable = false)
    private Idea ideaA;

    /** The second competing idea. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "idea_b_id", nullable = false)
    private Idea ideaB;

    /** Running vote tally for ideaA. */
    @Column(name = "votes_a", nullable = false)
    @Builder.Default
    private Integer votesA = 0;

    /** Running vote tally for ideaB. */
    @Column(name = "votes_b", nullable = false)
    @Builder.Default
    private Integer votesB = 0;

    /**
     * ID of the winning idea. Null until the battle is completed.
     * Not a FK relationship to allow flexibility (draws can be represented as null).
     */
    @Column(name = "winner_id")
    private Long winnerId;

    /** Scheduled end time of the battle. */
    @Column(name = "ends_at", nullable = false)
    private LocalDateTime endsAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private BattleStatus status = BattleStatus.ACTIVE;

    // ------------------------------------------------------------------
    // Nested enum
    // ------------------------------------------------------------------

    public enum BattleStatus { ACTIVE, COMPLETED }
}
