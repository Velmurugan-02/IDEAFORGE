package com.ideaforge.ideaforge_backend.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * A comment posted by a user on an idea.
 * Tagged with a contextual type (SUPPORT / QUESTION / CHALLENGE) to aid
 * idea creators in triaging community feedback.
 */
@Entity
@Table(name = "comments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Comment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "idea_id", nullable = false)
    private Idea idea;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    /** Semantic tag that categorises the nature of the comment. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private CommentTag tag = CommentTag.QUESTION;

    /** Marks whether the idea owner has replied / resolved this comment. */
    @Column(nullable = false)
    @Builder.Default
    private Boolean answered = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    // ------------------------------------------------------------------
    // Nested enum
    // ------------------------------------------------------------------

    public enum CommentTag { SUPPORT, QUESTION, CHALLENGE }
}
