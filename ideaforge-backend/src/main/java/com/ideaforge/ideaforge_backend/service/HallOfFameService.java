package com.ideaforge.ideaforge_backend.service;

import com.ideaforge.ideaforge_backend.dto.HallOfFameResponse;
import com.ideaforge.ideaforge_backend.model.HallOfFame;
import com.ideaforge.ideaforge_backend.model.Idea;
import com.ideaforge.ideaforge_backend.repository.HallOfFameRepository;
import com.ideaforge.ideaforge_backend.repository.IdeaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Manages the weekly Hall of Fame snapshot.
 *
 * <p>Every Monday at midnight:
 * <ol>
 *   <li>Select top 3 PUBLIC, non-plagiarised ideas by traction_score.</li>
 *   <li>Save entries in hall_of_fame with week_start, rank 1/2/3, snapshot_score.</li>
 *   <li>Award CROWD_FAVOURITE badge + HALL_OF_FAME XP (+300) to all 3 owners.</li>
 * </ol>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HallOfFameService {

    private final HallOfFameRepository hallOfFameRepository;
    private final IdeaRepository       ideaRepository;
    private final BadgeService         badgeService;
    private final XpService            xpService;

    // ------------------------------------------------------------------
    // Scheduled job — every Monday at 00:00:00
    // ------------------------------------------------------------------

    @Scheduled(cron = "0 0 0 * * MON")
    @Transactional
    public void runWeeklyHallOfFame() {
        LocalDate weekStart = LocalDate.now(); // Monday midnight = start of the new week
        log.info("📜 Running weekly Hall of Fame snapshot for week starting {}", weekStart);

        List<Idea> topIdeas = ideaRepository.findTopPublicNonPlagiarisedIdeas(PageRequest.of(0, 3));

        if (topIdeas.isEmpty()) {
            log.info("No eligible ideas for Hall of Fame this week.");
            return;
        }

        int rank = 1;
        for (Idea idea : topIdeas) {
            // Prevent duplicate entries if job somehow fires twice in the same week
            if (hallOfFameRepository.existsByIdeaIdAndWeekStart(idea.getId(), weekStart)) {
                log.warn("HoF entry already exists for idea {} week {}, skipping.", idea.getId(), weekStart);
                rank++;
                continue;
            }

            BigDecimal snapshotScore = idea.getTractionScore() != null
                    ? BigDecimal.valueOf(idea.getTractionScore())
                    : BigDecimal.ZERO;

            HallOfFame entry = HallOfFame.builder()
                    .idea(idea)
                    .weekStart(weekStart)
                    .rank(rank)
                    .snapshotScore(snapshotScore)
                    .build();
            hallOfFameRepository.save(entry);

            Long ownerId = idea.getUser().getId();

            // Award XP for entering Hall of Fame
            xpService.awardXP(ownerId, "HALL_OF_FAME");

            // Award CROWD_FAVOURITE badge (BadgeService checks for duplicates internally)
            badgeService.checkAndAwardBadges(ownerId);

            log.info("🏆 HoF rank {} → idea '{}' (id={}) owner id={}", rank, idea.getTitle(), idea.getId(), ownerId);
            rank++;
        }
    }

    // ------------------------------------------------------------------
    // Read API
    // ------------------------------------------------------------------

    /** Returns all Hall of Fame entries, ordered by week descending, rank ascending. */
    @Transactional(readOnly = true)
    public List<HallOfFameResponse> getAllEntries() {
        return hallOfFameRepository.findAllByOrderByWeekStartDescRankAsc().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private HallOfFameResponse mapToResponse(HallOfFame entry) {
        Idea idea = entry.getIdea();
        return HallOfFameResponse.builder()
                .id(entry.getId())
                .weekStart(entry.getWeekStart())
                .rank(entry.getRank())
                .snapshotScore(entry.getSnapshotScore())
                .ideaId(idea.getId())
                .ideaTitle(idea.getTitle())
                .ideaPitch(idea.getPitch())
                .category(idea.getCategory())
                .ownerId(idea.getUser().getId())
                .ownerName(idea.getUser().getName())
                .build();
    }
}
