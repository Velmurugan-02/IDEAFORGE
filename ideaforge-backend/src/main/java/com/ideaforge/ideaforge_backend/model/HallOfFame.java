package com.ideaforge.ideaforge_backend.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * A weekly snapshot of a top-ranked idea, stored permanently in the Hall of Fame.
 * Captures the idea's score at the moment of snapshot so historical rankings are preserved.
 */
@Entity
@Table(name = "hall_of_fame")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HallOfFame {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "idea_id", nullable = false)
    private Idea idea;

    /** Start date (Monday) of the week this entry represents. */
    @Column(name = "week_start", nullable = false)
    private LocalDate weekStart;

    /** Position within that week's leaderboard (1 = top). */
    @Column(name = "rank_position", nullable = false)
    private Integer rank;

    /** Traction score at the time of the weekly snapshot. */
    @Column(name = "snapshot_score", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal snapshotScore = BigDecimal.ZERO;
}
