package com.ideaforge.ideaforge_backend.repository;

import com.ideaforge.ideaforge_backend.model.CollabRequest;
import com.ideaforge.ideaforge_backend.model.CollabRequest.CollabStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for {@link CollabRequest} entities.
 */
@Repository
public interface CollabRequestRepository extends JpaRepository<CollabRequest, Long> {

    /** All collaboration requests received by an owner, newest first. */
    List<CollabRequest> findByOwnerIdOrderByCreatedAtDesc(Long ownerId);

    /** All collaboration requests sent BY a specific user, newest first. */
    List<CollabRequest> findByRequesterIdOrderByCreatedAtDesc(Long requesterId);

    /** All pending requests on a specific idea (for the owner's dashboard). */
    List<CollabRequest> findByIdeaIdAndStatus(Long ideaId, CollabStatus status);

    /** Check if a user has already sent a pending request for a given idea. */
    boolean existsByIdeaIdAndRequesterIdAndStatus(Long ideaId, Long requesterId, CollabStatus status);

    /** Check if a user is involved in at least one ACCEPTED collab (as requester OR owner — COLLABORATOR badge). */
    boolean existsByRequesterIdAndStatus(Long requesterId, CollabStatus status);

    /** Check if a user has accepted someone else's request (owner side — COLLABORATOR badge). */
    boolean existsByOwnerIdAndStatus(Long ownerId, CollabStatus status);
}
