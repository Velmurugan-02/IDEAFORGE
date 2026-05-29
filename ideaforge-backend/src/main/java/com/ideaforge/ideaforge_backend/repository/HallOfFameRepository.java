package com.ideaforge.ideaforge_backend.repository;

import com.ideaforge.ideaforge_backend.model.HallOfFame;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * Repository for {@link HallOfFame} weekly snapshot entries.
 */
@Repository
public interface HallOfFameRepository extends JpaRepository<HallOfFame, Long> {

    /** All entries for a specific week, ordered by rank ascending. */
    List<HallOfFame> findByWeekStartOrderByRankAsc(LocalDate weekStart);

    /** Hall of Fame history for a specific idea. */
    List<HallOfFame> findByIdeaIdOrderByWeekStartDesc(Long ideaId);

    /** Check if a weekly snapshot already exists for a given idea (prevents re-insertion). */
    boolean existsByIdeaIdAndWeekStart(Long ideaId, LocalDate weekStart);

    /** Check if any idea owned by a user is in the hall of fame (CROWD_FAVOURITE badge). */
    boolean existsByIdeaUserId(Long userId);

    /** All entries ordered by week descending then rank ascending (for GET /api/hall-of-fame). */
    List<HallOfFame> findAllByOrderByWeekStartDescRankAsc();
}
