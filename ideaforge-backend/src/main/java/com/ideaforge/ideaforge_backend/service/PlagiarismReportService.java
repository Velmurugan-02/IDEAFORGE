package com.ideaforge.ideaforge_backend.service;

import com.ideaforge.ideaforge_backend.dto.PlagiarismReportRequest;
import com.ideaforge.ideaforge_backend.dto.PlagiarismReportResponse;
import com.ideaforge.ideaforge_backend.model.Idea;
import com.ideaforge.ideaforge_backend.model.PlagiarismReport;
import com.ideaforge.ideaforge_backend.model.User;
import com.ideaforge.ideaforge_backend.repository.IdeaRepository;
import com.ideaforge.ideaforge_backend.repository.PlagiarismReportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PlagiarismReportService {

    private final PlagiarismReportRepository reportRepository;
    private final IdeaRepository ideaRepository;

    @Transactional
    public PlagiarismReportResponse submitReport(Long reportedIdeaId, PlagiarismReportRequest request, User currentUser) {
        if (reportRepository.existsByReportedIdeaIdAndReportedById(reportedIdeaId, currentUser.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You have already reported this idea");
        }

        Idea reportedIdea = ideaRepository.findById(reportedIdeaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Reported idea not found"));

        Idea originalIdea = ideaRepository.findById(request.getOriginalIdeaId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Original idea not found"));

        if (!originalIdea.getCreatedAt().isBefore(reportedIdea.getCreatedAt())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The idea you claimed as original was posted after the reported idea");
        }

        PlagiarismReport report = PlagiarismReport.builder()
                .reportedIdea(reportedIdea)
                .originalIdea(originalIdea)
                .reportedBy(currentUser)
                .reason(request.getReason())
                .status(PlagiarismReport.ReportStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();

        PlagiarismReport savedReport = reportRepository.save(report);
        return mapToResponse(savedReport);
    }

    public List<PlagiarismReportResponse> getReportsForIdea(Long ideaId) {
        return reportRepository.findByReportedIdeaId(ideaId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public PlagiarismReportResponse mapToResponse(PlagiarismReport report) {
        return PlagiarismReportResponse.builder()
                .id(report.getId())
                .reportedIdeaId(report.getReportedIdea().getId())
                .originalIdeaId(report.getOriginalIdea().getId())
                .reportedById(report.getReportedBy().getId())
                .reporterName(report.getReportedBy().getName())
                .reason(report.getReason())
                .status(report.getStatus().name())
                .createdAt(report.getCreatedAt())
                .build();
    }
}
