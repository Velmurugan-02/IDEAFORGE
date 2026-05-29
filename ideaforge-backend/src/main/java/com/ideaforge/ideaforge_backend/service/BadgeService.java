package com.ideaforge.ideaforge_backend.service;

import com.ideaforge.ideaforge_backend.model.Badge;
import com.ideaforge.ideaforge_backend.model.Comment;
import com.ideaforge.ideaforge_backend.model.Idea;
import com.ideaforge.ideaforge_backend.model.User;
import com.ideaforge.ideaforge_backend.model.CollabRequest.CollabStatus;
import com.ideaforge.ideaforge_backend.model.Idea.BattleStatus;
import com.ideaforge.ideaforge_backend.model.PlagiarismReport.ReportStatus;
import com.ideaforge.ideaforge_backend.repository.BadgeRepository;
import com.ideaforge.ideaforge_backend.repository.CollabRequestRepository;
import com.ideaforge.ideaforge_backend.repository.CommentRepository;
import com.ideaforge.ideaforge_backend.repository.HallOfFameRepository;
import com.ideaforge.ideaforge_backend.repository.IdeaRepository;
import com.ideaforge.ideaforge_backend.repository.PlagiarismReportRepository;
import com.ideaforge.ideaforge_backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Badge engine for the IdeaForge gamification system.
 *
 * <p>Badge types and their conditions:
 * <ul>
 *   <li><b>FIRST_IDEA</b>        — user posts their very first idea</li>
 *   <li><b>SERIAL_PITCHER</b>    — user posts 5 or more ideas total</li>
 *   <li><b>TRENDSETTER</b>       — user has an idea with vote_count ≥ 100</li>
 *   <li><b>PROBLEM_SOLVER</b>    — user has an idea with battle_status = WON</li>
 *   <li><b>GHOST_BUSTER</b>      — user has answered ALL CHALLENGE comments on at least one idea</li>
 *   <li><b>CROWD_FAVOURITE</b>   — user's idea appeared in the hall_of_fame table</li>
 *   <li><b>PROTECTOR</b>         — user has a CONFIRMED plagiarism report to their name</li>
 *   <li><b>COLLABORATOR</b>      — user is involved in at least one ACCEPTED collab (either side)</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BadgeService {

    private final BadgeRepository            badgeRepository;
    private final UserRepository             userRepository;
    private final IdeaRepository             ideaRepository;
    private final CommentRepository          commentRepository;
    private final PlagiarismReportRepository reportRepository;
    private final CollabRequestRepository    collabRequestRepository;
    private final HallOfFameRepository       hallOfFameRepository;

    // ------------------------------------------------------------------
    // Main entry point — called after any action that could unlock a badge
    // ------------------------------------------------------------------

    @Transactional
    public void checkAndAwardBadges(Long userId) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) return;

        checkFirstIdea(user);
        checkSerialPitcher(user);
        checkTrendsetter(user);
        checkProblemSolver(user);
        checkGhostBuster(user);
        checkCrowdFavourite(user);
        checkProtector(user);
        checkCollaborator(user);
    }

    // ------------------------------------------------------------------
    // Individual badge checkers
    // ------------------------------------------------------------------

    /** FIRST_IDEA — awarded when user posts their very first idea (count == 1). */
    private void checkFirstIdea(User user) {
        if (alreadyEarned(user.getId(), "FIRST_IDEA")) return;
        if (ideaRepository.countByUserId(user.getId()) >= 1) {
            award(user, "FIRST_IDEA");
        }
    }

    /** SERIAL_PITCHER — 5 or more ideas posted. */
    private void checkSerialPitcher(User user) {
        if (alreadyEarned(user.getId(), "SERIAL_PITCHER")) return;
        if (ideaRepository.countByUserId(user.getId()) >= 5) {
            award(user, "SERIAL_PITCHER");
        }
    }

    /** TRENDSETTER — any idea with vote_count >= 100. */
    private void checkTrendsetter(User user) {
        if (alreadyEarned(user.getId(), "TRENDSETTER")) return;
        if (ideaRepository.existsByUserIdAndVoteCountGreaterThanEqual(user.getId(), 100)) {
            award(user, "TRENDSETTER");
        }
    }

    /** PROBLEM_SOLVER — user has an idea with battle_status = WON. */
    private void checkProblemSolver(User user) {
        if (alreadyEarned(user.getId(), "PROBLEM_SOLVER")) return;
        if (ideaRepository.existsByUserIdAndBattleStatus(user.getId(), BattleStatus.WON)) {
            award(user, "PROBLEM_SOLVER");
        }
    }

    /**
     * GHOST_BUSTER — user has at least one idea where ALL CHALLENGE comments are answered.
     * We find all the user's ideas and check if any has zero unanswered CHALLENGE comments
     * (while having at least one CHALLENGE comment, so they actually needed to answer).
     */
    private void checkGhostBuster(User user) {
        if (alreadyEarned(user.getId(), "GHOST_BUSTER")) return;

        List<Idea> userIdeas = ideaRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
        for (Idea idea : userIdeas) {
            long totalChallenges = commentRepository.countByIdeaIdAndTagAndAnswered(
                    idea.getId(), Comment.CommentTag.CHALLENGE, true)
                    + commentRepository.countByIdeaIdAndTagAndAnswered(
                    idea.getId(), Comment.CommentTag.CHALLENGE, false);

            if (totalChallenges == 0) continue; // skip ideas with no challenges

            long unanswered = commentRepository.countByIdeaIdAndTagAndAnswered(
                    idea.getId(), Comment.CommentTag.CHALLENGE, false);

            if (unanswered == 0) {
                award(user, "GHOST_BUSTER");
                return;
            }
        }
    }

    /** CROWD_FAVOURITE — user's idea appeared in the hall_of_fame table. */
    private void checkCrowdFavourite(User user) {
        if (alreadyEarned(user.getId(), "CROWD_FAVOURITE")) return;
        if (hallOfFameRepository.existsByIdeaUserId(user.getId())) {
            award(user, "CROWD_FAVOURITE");
        }
    }

    /** PROTECTOR — user has at least one CONFIRMED plagiarism report. */
    private void checkProtector(User user) {
        if (alreadyEarned(user.getId(), "PROTECTOR")) return;
        if (reportRepository.existsByReportedByIdAndStatus(user.getId(), ReportStatus.CONFIRMED)) {
            award(user, "PROTECTOR");
        }
    }

    /** COLLABORATOR — user has an ACCEPTED collab as requester OR as owner. */
    private void checkCollaborator(User user) {
        if (alreadyEarned(user.getId(), "COLLABORATOR")) return;
        boolean isRequester = collabRequestRepository.existsByRequesterIdAndStatus(user.getId(), CollabStatus.ACCEPTED);
        boolean isOwner     = collabRequestRepository.existsByOwnerIdAndStatus(user.getId(), CollabStatus.ACCEPTED);
        if (isRequester || isOwner) {
            award(user, "COLLABORATOR");
        }
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private boolean alreadyEarned(Long userId, String type) {
        return badgeRepository.existsByUserIdAndType(userId, type);
    }

    private void award(User user, String type) {
        Badge badge = Badge.builder()
                .user(user)
                .type(type)
                .build();
        badgeRepository.save(badge);
        log.info("🏅 Badge '{}' awarded to user {} (id={})", type, user.getName(), user.getId());
    }
}
