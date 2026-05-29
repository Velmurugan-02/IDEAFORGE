package com.ideaforge.ideaforge_backend.repository;

import com.ideaforge.ideaforge_backend.model.BattleVote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for {@link BattleVote} entities.
 */
@Repository
public interface BattleVoteRepository extends JpaRepository<BattleVote, Long> {
    boolean existsByBattleIdAndUserId(Long battleId, Long userId);
}
