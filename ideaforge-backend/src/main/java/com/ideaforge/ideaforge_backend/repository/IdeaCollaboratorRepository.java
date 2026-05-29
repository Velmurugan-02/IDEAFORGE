package com.ideaforge.ideaforge_backend.repository;

import com.ideaforge.ideaforge_backend.model.IdeaCollaborator;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IdeaCollaboratorRepository extends JpaRepository<IdeaCollaborator, Long> {
    List<IdeaCollaborator> findByIdeaId(Long ideaId);

    List<IdeaCollaborator> findByUserIdOrderByJoinedAtDesc(Long userId);

    boolean existsByIdeaIdAndUserId(Long ideaId, Long userId);
}
