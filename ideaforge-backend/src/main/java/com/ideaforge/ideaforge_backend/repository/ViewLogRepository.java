package com.ideaforge.ideaforge_backend.repository;

import com.ideaforge.ideaforge_backend.model.ViewLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ViewLogRepository extends JpaRepository<ViewLog, Long> {

    /**
     * Find today's view log for a specific viewer on a specific idea.
     * "Today" means the viewedAt timestamp is >= the start of the calendar day.
     */
    @Query("""
            SELECT v FROM ViewLog v
            WHERE v.idea.id = :ideaId
              AND v.viewer.id = :viewerId
              AND v.viewedAt >= :dayStart
            """)
    Optional<ViewLog> findTodayEntry(
            @Param("ideaId")  Long ideaId,
            @Param("viewerId") Long viewerId,
            @Param("dayStart") LocalDateTime dayStart);

    /** All view logs for an idea (used by the admin evidence view). */
    List<ViewLog> findByIdeaIdOrderByViewedAtDesc(Long ideaId);

    /** Count distinct viewers for an idea. */
    @Query("SELECT COUNT(DISTINCT v.viewer.id) FROM ViewLog v WHERE v.idea.id = :ideaId")
    long countUniqueViewers(@Param("ideaId") Long ideaId);

    /** Count total view entries (not unique) for an idea. */
    long countByIdeaId(Long ideaId);

    /** Count views today for an idea. */
    @Query("""
            SELECT COUNT(v) FROM ViewLog v
            WHERE v.idea.id = :ideaId
              AND v.viewedAt >= :dayStart
            """)
    long countViewsToday(@Param("ideaId") Long ideaId, @Param("dayStart") LocalDateTime dayStart);

    /** Count views this week for an idea. */
    @Query("""
            SELECT COUNT(v) FROM ViewLog v
            WHERE v.idea.id = :ideaId
              AND v.viewedAt >= :weekStart
            """)
    long countViewsThisWeek(@Param("ideaId") Long ideaId, @Param("weekStart") LocalDateTime weekStart);

    /** Count viewers who gave consent for an idea. */
    @Query("SELECT COUNT(v) FROM ViewLog v WHERE v.idea.id = :ideaId AND v.consentGiven = true")
    long countConsentedViewers(@Param("ideaId") Long ideaId);

    /** Check whether a viewer has ever consented for an idea. */
    @Query("""
            SELECT COUNT(v) > 0 FROM ViewLog v
            WHERE v.idea.id = :ideaId
              AND v.viewer.id = :viewerId
              AND v.consentGiven = true
            """)
    boolean hasConsented(@Param("ideaId") Long ideaId, @Param("viewerId") Long viewerId);
}
