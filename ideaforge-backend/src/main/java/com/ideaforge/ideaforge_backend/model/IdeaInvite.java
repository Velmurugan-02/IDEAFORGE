package com.ideaforge.ideaforge_backend.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * An invitation token that grants access to a PRIVATE or INVITE_ONLY idea.
 * Tokens are single-use and expire after a configurable period.
 */
@Entity
@Table(name = "idea_invites")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IdeaInvite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The idea being shared via this invite. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "idea_id", nullable = false)
    private Idea idea;

    /** The user who generated this invite link. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "invited_by", nullable = false)
    private User invitedBy;

    /** Unique opaque token included in the invite URL. */
    @Column(name = "invite_token", nullable = false, unique = true, length = 255)
    private String inviteToken;

    /** When this token expires and is no longer valid. */
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    /** True once the token has been redeemed (single-use). */
    @Column(nullable = false)
    @Builder.Default
    private Boolean used = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
