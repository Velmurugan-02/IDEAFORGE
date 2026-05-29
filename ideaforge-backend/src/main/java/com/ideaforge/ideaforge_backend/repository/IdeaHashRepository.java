package com.ideaforge.ideaforge_backend.repository;

import com.ideaforge.ideaforge_backend.model.IdeaHash;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface IdeaHashRepository extends JpaRepository<IdeaHash, Long> {

    Optional<IdeaHash> findByIdeaId(Long ideaId);

    boolean existsByIdeaId(Long ideaId);
}
