package com.ideaforge.ideaforge_backend.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * A badge awarded to a user for achieving a milestone or completing a challenge
 * within the IdeaForge gamification system.
 *
 * Example badge types: "FIRST_IDEA", "BATTLE_WINNER", "TOP_VOTER", "WEEK_CHAMPION"
 */
@Entity
@Table(name = "badges")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Badge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** Badge identifier string (corresponds to a badge definition in the frontend). */
    @Column(nullable = false, length = 50)
    private String type;

    @Column(name = "earned_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime earnedAt = LocalDateTime.now();
}
