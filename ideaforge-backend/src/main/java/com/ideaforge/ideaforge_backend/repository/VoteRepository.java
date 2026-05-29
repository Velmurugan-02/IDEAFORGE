package com.ideaforge.ideaforge_backend.repository;

import com.ideaforge.ideaforge_backend.model.Vote;
import com.ideaforge.ideaforge_backend.model.Vote.VoteType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for {@link Vote} entities.
 */
@Repository
public interface VoteRepository extends JpaRepository<Vote, Long> {

    /** Retrieve an existing vote cast by a specific user on a specific idea. */
    Optional<Vote> findByIdeaIdAndUserId(Long ideaId, Long userId);

    /** Check if a user has already voted on an idea. */
    boolean existsByIdeaIdAndUserId(Long ideaId, Long userId);

    /** Count all UP votes for a given idea. */
    long countByIdeaIdAndType(Long ideaId, VoteType type);

    /** Total number of votes across an idea (UP + DOWN). */
    @Query("SELECT COUNT(v) FROM Vote v WHERE v.idea.id = :ideaId")
    long countAllByIdeaId(@Param("ideaId") Long ideaId);
}
