package com.ideaforge.ideaforge_backend.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * A collaboration request sent by a user who wants to work on someone else's idea.
 * The idea owner can ACCEPT or REJECT the request.
 */
@Entity
@Table(name = "collab_requests")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CollabRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The idea the requester wants to collaborate on. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "idea_id", nullable = false)
    private Idea idea;

    /** The user who sent the collaboration request. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "requester_id", nullable = false)
    private User requester;

    /** The idea owner who will act on this request. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    /** Optional message from the requester explaining why they want to collaborate. */
    @Column(columnDefinition = "TEXT")
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private CollabStatus status = CollabStatus.PENDING;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    // ------------------------------------------------------------------
    // Nested enum
    // ------------------------------------------------------------------

    public enum CollabStatus { PENDING, ACCEPTED, REJECTED }
}
