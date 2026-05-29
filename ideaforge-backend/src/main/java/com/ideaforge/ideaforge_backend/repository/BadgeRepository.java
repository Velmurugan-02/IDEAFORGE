package com.ideaforge.ideaforge_backend.repository;

import com.ideaforge.ideaforge_backend.model.Badge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for {@link Badge} entities.
 */
@Repository
public interface BadgeRepository extends JpaRepository<Badge, Long> {

    /** All badges earned by a specific user, newest first. */
    List<Badge> findByUserIdOrderByEarnedAtDesc(Long userId);

    /** Check if a user already holds a particular badge type (prevents duplicates). */
    boolean existsByUserIdAndType(Long userId, String type);
}
