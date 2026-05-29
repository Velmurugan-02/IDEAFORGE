package com.ideaforge.ideaforge_backend.service;

import com.ideaforge.ideaforge_backend.dto.AdminPlatformStatsResponse;
import com.ideaforge.ideaforge_backend.dto.AdminReportResponse;
import com.ideaforge.ideaforge_backend.model.Battle;
import com.ideaforge.ideaforge_backend.model.Idea;
import com.ideaforge.ideaforge_backend.model.PlagiarismReport;
import com.ideaforge.ideaforge_backend.model.User;
import com.ideaforge.ideaforge_backend.repository.BattleRepository;
import com.ideaforge.ideaforge_backend.repository.IdeaRepository;
import com.ideaforge.ideaforge_backend.repository.PlagiarismReportRepository;
import com.ideaforge.ideaforge_backend.repository.UserRepository;
import com.ideaforge.ideaforge_backend.repository.VoteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.messaging.simp.SimpMessagingTemplate;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final PlagiarismReportRepository reportRepository;
    private final IdeaRepository ideaRepository;
    private final PlagiarismReportService reportService;
    private final SimpMessagingTemplate messagingTemplate;
    private final BadgeService badgeService;
    private final IdeaService ideaService;
    private final VoteRepository voteRepository;
    private final BattleRepository battleRepository;

    private com.ideaforge.ideaforge_backend.dto.IdeaResponse mapIdeaToResponse(Idea idea) {
        return ideaService.mapToResponse(idea);
    }

    @Transactional
    public void setupFirstAdmin(User currentUser) {
        long adminCount = userRepository.findAll().stream()
                .filter(u -> "ROLE_ADMIN".equals(u.getRole()))
                .count();

        if (adminCount > 0) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin already exists. Setup disabled.");
        }

        User user = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        
        user.setRole("ROLE_ADMIN");
        userRepository.save(user);
    }

    public List<AdminReportResponse> getPendingReports() {
        return reportRepository.findByStatusOrderByCreatedAtAsc(PlagiarismReport.ReportStatus.PENDING).stream()
                .map(report -> AdminReportResponse.builder()
                        .report(reportService.mapToResponse(report))
                        .reportedIdea(mapIdeaToResponse(report.getReportedIdea()))
                        .originalIdea(mapIdeaToResponse(report.getOriginalIdea()))
                        .build())
                .collect(Collectors.toList());
    }

    public AdminPlatformStatsResponse getPlatformStats() {
        return AdminPlatformStatsResponse.builder()
                .totalUsers(userRepository.count())
                .totalIdeas(ideaRepository.count())
                .totalVotes(voteRepository.count())
                .totalBattlesFought(battleRepository.countByStatus(Battle.BattleStatus.COMPLETED))
                .build();
    }

    @Transactional
    public void confirmReport(Long reportId) {
        PlagiarismReport report = reportRepository.findById(reportId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Report not found"));

        report.setStatus(PlagiarismReport.ReportStatus.CONFIRMED);
        reportRepository.save(report);

        Idea reportedIdea = report.getReportedIdea();
        reportedIdea.setIsPlagiarised(true);
        ideaRepository.save(reportedIdea);

        // Broadcast notification to the owner
        Map<String, Object> notificationPayload = Map.of(
                "type", "IDEA_FLAGGED",
                "message", "Your idea '" + reportedIdea.getTitle() + "' has been flagged as plagiarised"
        );
        messagingTemplate.convertAndSend("/topic/user/" + reportedIdea.getUser().getId() + "/notifications", notificationPayload);

        // Award PROTECTOR badge to the reporter
        badgeService.checkAndAwardBadges(report.getReportedBy().getId());
    }

    @Transactional
    public void dismissReport(Long reportId) {
        PlagiarismReport report = reportRepository.findById(reportId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Report not found"));

        report.setStatus(PlagiarismReport.ReportStatus.DISMISSED);
        reportRepository.save(report);
    }
}
