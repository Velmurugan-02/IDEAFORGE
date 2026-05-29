package com.ideaforge.ideaforge_backend.repository;

import com.ideaforge.ideaforge_backend.model.IdeaCertificate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for {@link IdeaCertificate} entities.
 */
@Repository
public interface IdeaCertificateRepository extends JpaRepository<IdeaCertificate, Long> {

    /** Retrieve the certificate for a specific idea (null if not yet issued). */
    Optional<IdeaCertificate> findByIdeaId(Long ideaId);

    /** Look up a certificate by its human-readable code (for public verification pages). */
    Optional<IdeaCertificate> findByCertificateCode(String certificateCode);

    /** Check whether an idea has already been issued a certificate. */
    boolean existsByIdeaId(Long ideaId);
}
