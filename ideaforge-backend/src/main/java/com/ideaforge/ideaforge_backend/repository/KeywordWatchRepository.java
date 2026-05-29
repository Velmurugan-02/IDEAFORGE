package com.ideaforge.ideaforge_backend.repository;

import com.ideaforge.ideaforge_backend.model.KeywordWatch;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface KeywordWatchRepository extends JpaRepository<KeywordWatch, Long> {

    List<KeywordWatch> findByIdeaId(Long ideaId);

    /** Total keyword count for an idea — used to enforce the 5-keyword limit. */
    long countByIdeaId(Long ideaId);

    /** All watches across all ideas — used by the scheduled scanner. */
    List<KeywordWatch> findAll();
}
