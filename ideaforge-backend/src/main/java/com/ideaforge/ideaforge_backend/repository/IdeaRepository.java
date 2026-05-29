package com.ideaforge.ideaforge_backend.repository;

import com.ideaforge.ideaforge_backend.model.Idea;
import com.ideaforge.ideaforge_backend.model.Idea.BattleStatus;
import com.ideaforge.ideaforge_backend.model.Idea.Visibility;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for {@link Idea} entities.
 */
@Repository
public interface IdeaRepository extends JpaRepository<Idea, Long> {

    /** All ideas belonging to a specific user, newest first. */
    List<Idea> findByUserIdOrderByCreatedAtDesc(Long userId);

    /** Paginated feed of publicly visible ideas, sorted by traction score descending. */
    Page<Idea> findByVisibilityOrderByTractionScoreDesc(Visibility visibility, Pageable pageable);

    /** Fetch ideas by visibility (for stable pagination in scheduled jobs). */
    Page<Idea> findByVisibility(Visibility visibility, Pageable pageable);

    /** Search public ideas by title keyword (case-insensitive). */
    @Query("SELECT i FROM Idea i WHERE i.visibility = 'PUBLIC' AND LOWER(i.title) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<Idea> searchPublicByTitle(@Param("keyword") String keyword);

    /** Ideas in a given battle status (e.g. ACTIVE battles). */
    List<Idea> findByBattleStatus(BattleStatus battleStatus);

    /** All public ideas in a specific category, ordered by score. */
    List<Idea> findByCategoryAndVisibilityOrderByTractionScoreDesc(String category, Visibility visibility);

    /** Top N ideas by traction score for Hall-of-Fame nomination jobs. */
    @Query("SELECT i FROM Idea i WHERE i.visibility = 'PUBLIC' ORDER BY i.tractionScore DESC")
    List<Idea> findTopIdeasByScore(Pageable pageable);

    /** Fetch top ideas for the live leaderboard (excluding plagiarised ones). */
    Page<Idea> findByVisibilityAndIsPlagiarisedFalseOrderByTractionScoreDesc(Visibility visibility, Pageable pageable);

    /** Filter ideas by category and visibility (PUBLIC or owned by user). */
    @Query("SELECT i FROM Idea i WHERE " +
           "(:category IS NULL OR i.category = :category) AND " +
           "(i.visibility = 'PUBLIC' OR (:userId IS NOT NULL AND i.user.id = :userId))")
    Page<Idea> findIdeasWithFilters(@Param("category") String category, @Param("userId") Long userId, Pageable pageable);

    /** Count total ideas posted by a user (for SERIAL_PITCHER / FIRST_IDEA badges). */
    long countByUserId(Long userId);

    /** Check if a user has at least one idea with the given battle status (PROBLEM_SOLVER badge). */
    boolean existsByUserIdAndBattleStatus(Long userId, BattleStatus battleStatus);

    /** Top N PUBLIC, non-plagiarised ideas by traction score (Hall of Fame weekly cron). */
    @Query("SELECT i FROM Idea i WHERE i.visibility = 'PUBLIC' AND i.isPlagiarised = false ORDER BY i.tractionScore DESC")
    List<Idea> findTopPublicNonPlagiarisedIdeas(Pageable pageable);

    /** Check if any idea by user has vote_count >= threshold (TRENDSETTER badge). */
    boolean existsByUserIdAndVoteCountGreaterThanEqual(Long userId, int threshold);

    /** All public non-plagiarised ideas for duplicate-check scanning (no pagination). */
    @Query("SELECT i FROM Idea i WHERE i.visibility = 'PUBLIC' AND i.isPlagiarised = false ORDER BY i.tractionScore DESC")
    List<Idea> findAllPublicNonPlagiarised();

    /** Paginated ideas by category (any visibility — filtered downstream). */
    Page<Idea> findByCategory(String category, Pageable pageable);
}
