package com.ideaforge.ideaforge_backend.repository;

import com.ideaforge.ideaforge_backend.model.KeywordAlert;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface KeywordAlertRepository extends JpaRepository<KeywordAlert, Long> {

    /** All unread alerts for a user, newest first. */
    List<KeywordAlert> findByUserIdAndIsReadFalseOrderByDetectedAtDesc(Long userId);

    /** All alerts for a user (read + unread), newest first. */
    List<KeywordAlert> findByUserIdOrderByDetectedAtDesc(Long userId);

    /** All alerts triggered by a specific keyword watch. */
    List<KeywordAlert> findByWatchId(Long watchId);

    /** Check if an alert already exists for a watch + source URL (dedup). */
    boolean existsByWatchIdAndSourceUrl(Long watchId, String sourceUrl);
}
