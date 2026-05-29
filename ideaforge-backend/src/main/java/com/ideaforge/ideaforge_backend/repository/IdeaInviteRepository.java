package com.ideaforge.ideaforge_backend.repository;

import com.ideaforge.ideaforge_backend.model.IdeaInvite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for {@link IdeaInvite} token entities.
 */
@Repository
public interface IdeaInviteRepository extends JpaRepository<IdeaInvite, Long> {

    /** Look up an invite by its unique opaque token string (used when a user clicks the link). */
    Optional<IdeaInvite> findByInviteToken(String inviteToken);

    /** Count of active (unused, not expired) invites for an idea. */
    long countByIdeaIdAndUsedFalse(Long ideaId);

    /** List all invites for a specific idea. */
    List<IdeaInvite> findByIdeaIdOrderByCreatedAtDesc(Long ideaId);
}
