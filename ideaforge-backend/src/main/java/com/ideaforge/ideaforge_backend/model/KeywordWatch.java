package com.ideaforge.ideaforge_backend.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * A keyword registered by an idea owner to monitor the web for mentions
 * of their idea's core concepts.
 * Max 5 keywords per idea — enforced in the service layer.
 */
@Entity
@Table(name = "keyword_watches", indexes = {
        @Index(name = "idx_kw_idea_id", columnList = "idea_id"),
        @Index(name = "idx_kw_user_id", columnList = "user_id")
})
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class KeywordWatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The idea this keyword is protecting. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "idea_id", nullable = false)
    private Idea idea;

    /** The user (owner) who registered this keyword. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** The keyword or short phrase to monitor (max 100 characters). */
    @Column(nullable = false, length = 100)
    private String keyword;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
