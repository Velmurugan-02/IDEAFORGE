package com.ideaforge.ideaforge_backend.service;

import com.ideaforge.ideaforge_backend.dto.LevelUpEvent;
import com.ideaforge.ideaforge_backend.dto.VoteRequest;
import com.ideaforge.ideaforge_backend.model.Idea;
import com.ideaforge.ideaforge_backend.model.User;
import com.ideaforge.ideaforge_backend.model.Vote;
import com.ideaforge.ideaforge_backend.repository.IdeaRepository;
import com.ideaforge.ideaforge_backend.repository.VoteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class VoteService {

    private final VoteRepository voteRepository;
    private final IdeaRepository ideaRepository;
    private final TractionScoreService tractionScoreService;
    private final XpService xpService;
    private final UserService userService;
    private final BadgeService badgeService;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Casts (or toggles / switches) a vote. Returns a LevelUpEvent for the
     * <em>voting user</em> based on streak update.
     * Note: RECEIVED_UPVOTE XP goes to the idea owner — that event is not
     * returned here because the current user is the voter, not the owner.
     */
    @Transactional
    public LevelUpEvent castVote(Long ideaId, VoteRequest request, User currentUser) {
        Idea idea = ideaRepository.findById(ideaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Idea not found"));

        Vote.VoteType requestedType;
        try {
            requestedType = Vote.VoteType.valueOf(request.getType().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid vote type");
        }

        Optional<Vote> existingVoteOpt = voteRepository.findByIdeaIdAndUserId(ideaId, currentUser.getId());

        if (existingVoteOpt.isPresent()) {
            Vote existingVote = existingVoteOpt.get();
            if (existingVote.getType() == requestedType) {
                // Toggle off — no XP event for un-voting
                voteRepository.delete(existingVote);
                updateIdeaVoteCountAndTraction(idea, null);
                return buildNoOpEvent(currentUser);
            } else {
                // Switch vote direction
                existingVote.setType(requestedType);
                voteRepository.save(existingVote);
                updateIdeaVoteCountAndTraction(idea, requestedType.name());
                if (requestedType == Vote.VoteType.UP) {
                    xpService.awardXP(idea.getUser().getId(), "RECEIVED_UPVOTE");
                    badgeService.checkAndAwardBadges(idea.getUser().getId()); // TRENDSETTER check
                }
                // Streak update for the voting user
                return userService.updateStreak(currentUser.getId())
                        .orElse(buildNoOpEvent(currentUser));
            }
        }

        // New vote
        Vote newVote = Vote.builder()
                .idea(idea)
                .user(currentUser)
                .type(requestedType)
                .createdAt(LocalDateTime.now())
                .build();

        voteRepository.save(newVote);
        updateIdeaVoteCountAndTraction(idea, requestedType.name());

        if (requestedType == Vote.VoteType.UP) {
            xpService.awardXP(idea.getUser().getId(), "RECEIVED_UPVOTE");
            badgeService.checkAndAwardBadges(idea.getUser().getId()); // TRENDSETTER check
        }

        // Streak update for the voting user
        return userService.updateStreak(currentUser.getId())
                .orElse(buildNoOpEvent(currentUser));
    }

    public String getUserVoteType(Long ideaId, User currentUser) {
        return voteRepository.findByIdeaIdAndUserId(ideaId, currentUser.getId())
                .map(vote -> vote.getType().name())
                .orElse(null);
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private void updateIdeaVoteCountAndTraction(Idea idea, String userVote) {
        long upVotes   = voteRepository.countByIdeaIdAndType(idea.getId(), Vote.VoteType.UP);
        long downVotes = voteRepository.countByIdeaIdAndType(idea.getId(), Vote.VoteType.DOWN);
        int totalScore = (int) (upVotes - downVotes);

        idea.setVoteCount(totalScore);
        ideaRepository.save(idea);
        tractionScoreService.recalculateForIdea(idea.getId());

        Map<String, Object> payload = Map.of(
                "ideaId",    idea.getId(),
                "voteCount", totalScore,
                "upvotes",   upVotes,
                "downvotes", downVotes,
                "userVote",  userVote != null ? userVote : "NONE"
        );
        messagingTemplate.convertAndSend("/topic/idea/" + idea.getId() + "/votes", payload);
    }

    /** Returns a neutral LevelUpEvent (no XP gained, no level-up) for the current user. */
    private LevelUpEvent buildNoOpEvent(User user) {
        return LevelUpEvent.builder()
                .leveledUp(false)
                .newLevel(user.getLevel())
                .xpGained(0)
                .totalXP(user.getXp())
                .build();
    }
}
