package com.ideaforge.ideaforge_backend.repository;

import com.ideaforge.ideaforge_backend.model.PlagiarismReport;
import com.ideaforge.ideaforge_backend.model.PlagiarismReport.ReportStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for {@link PlagiarismReport} entities.
 */
@Repository
public interface PlagiarismReportRepository extends JpaRepository<PlagiarismReport, Long> {

    /** All reports by current moderation status (e.g. all PENDING reports for the mod queue). */
    List<PlagiarismReport> findByStatusOrderByCreatedAtAsc(ReportStatus status);

    /** All reports filed against a specific idea (regardless of status). */
    List<PlagiarismReport> findByReportedIdeaId(Long reportedIdeaId);

    /** All reports submitted by a specific user. */
    List<PlagiarismReport> findByReportedByIdOrderByCreatedAtDesc(Long reportedById);

    /** Check if a user has already reported a specific idea to prevent duplicate filings. */
    boolean existsByReportedIdeaIdAndReportedById(Long reportedIdeaId, Long reportedById);

    /** Check if a user has at least one CONFIRMED report (PROTECTOR badge). */
    boolean existsByReportedByIdAndStatus(Long reportedById, ReportStatus status);
}
