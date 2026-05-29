package com.ideaforge.ideaforge_backend.repository;

import com.ideaforge.ideaforge_backend.model.Battle;
import com.ideaforge.ideaforge_backend.model.Battle.BattleStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for {@link Battle} entities.
 */
@Repository
public interface BattleRepository extends JpaRepository<Battle, Long> {

    /** All battles that are currently active. */
    List<Battle> findByStatus(BattleStatus status);

    long countByStatus(BattleStatus status);

    /** Battles that have passed their end time but are still marked ACTIVE (for a scheduler). */
    @Query("SELECT b FROM Battle b WHERE b.status = 'ACTIVE' AND b.endsAt <= :now")
    List<Battle> findExpiredActiveBattles(@Param("now") LocalDateTime now);

    /** Battles involving a specific idea (either side). */
    @Query("SELECT b FROM Battle b WHERE b.ideaA.id = :ideaId OR b.ideaB.id = :ideaId")
    List<Battle> findBattlesByIdeaId(@Param("ideaId") Long ideaId);
}
