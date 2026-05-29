package com.ideaforge.ideaforge_backend.repository;

import com.ideaforge.ideaforge_backend.model.Comment;
import com.ideaforge.ideaforge_backend.model.Comment.CommentTag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for {@link Comment} entities.
 */
@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {

    /** All comments on an idea, sorted by creation date descending (newest first). */
    Page<Comment> findByIdeaIdOrderByCreatedAtDesc(Long ideaId, Pageable pageable);

    /** Filter comments on an idea by semantic tag, newest first. */
    List<Comment> findByIdeaIdAndTagOrderByCreatedAtDesc(Long ideaId, CommentTag tag);

    /** All unanswered comments on an idea – used by owners to see outstanding questions. */
    List<Comment> findByIdeaIdAndAnsweredFalseOrderByCreatedAtAsc(Long ideaId);

    /** Count of unanswered comments of a specific tag (e.g. CHALLENGE) for traction penalties. */
    long countByIdeaIdAndTagAndAnsweredFalse(Long ideaId, CommentTag tag);

    /** Count of comments for a given idea (for quick display without loading all). */
    long countByIdeaId(Long ideaId);

    /** All unanswered CHALLENGE comments on ideas owned by a user (for GHOST_BUSTER badge check). */
    @org.springframework.data.jpa.repository.Query(
        "SELECT COUNT(c) FROM Comment c WHERE c.idea.user.id = :userId AND c.tag = 'CHALLENGE' AND c.answered = false")
    long countUnansweredChallengesByOwner(@org.springframework.data.repository.query.Param("userId") Long userId);

    /** Count unanswered CHALLENGEs on a specific idea (to check single-idea GHOST_BUSTER condition). */
    long countByIdeaIdAndTagAndAnswered(Long ideaId, CommentTag tag, Boolean answered);
}
