package com.ideaforge.ideaforge_backend.service;

import com.ideaforge.ideaforge_backend.dto.BattleCreateRequest;
import com.ideaforge.ideaforge_backend.dto.BattleResponse;
import com.ideaforge.ideaforge_backend.dto.BattleWebSocketResponse;
import com.ideaforge.ideaforge_backend.dto.LevelUpEvent;
import com.ideaforge.ideaforge_backend.model.Battle;
import com.ideaforge.ideaforge_backend.model.BattleVote;
import com.ideaforge.ideaforge_backend.model.Idea;
import com.ideaforge.ideaforge_backend.model.User;
import com.ideaforge.ideaforge_backend.repository.BattleRepository;
import com.ideaforge.ideaforge_backend.repository.BattleVoteRepository;
import com.ideaforge.ideaforge_backend.repository.IdeaRepository;
import com.ideaforge.ideaforge_backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BattleService {

    private final BattleRepository battleRepository;
    private final BattleVoteRepository battleVoteRepository;
    private final IdeaRepository ideaRepository;
    private final UserRepository userRepository;
    private final IdeaService ideaService;
    private final XpService xpService;
    private final BadgeService badgeService;
    private final UserService userService;
    private final SimpMessagingTemplate messagingTemplate;

    @Transactional
    public BattleResponse createBattle(BattleCreateRequest request) {
        List<Battle> activeBattles = battleRepository.findByStatus(Battle.BattleStatus.ACTIVE);
        if (!activeBattles.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "An active battle already exists");
        }

        Idea ideaA = ideaRepository.findById(request.getIdeaAId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Idea A not found"));
        Idea ideaB = ideaRepository.findById(request.getIdeaBId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Idea B not found"));

        if (ideaA.getVisibility() != Idea.Visibility.PUBLIC || ideaB.getVisibility() != Idea.Visibility.PUBLIC) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Both ideas must be PUBLIC");
        }

        if (ideaA.getIsPlagiarised() || ideaB.getIsPlagiarised()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ideas cannot be plagiarised");
        }

        ideaA.setBattleStatus(Idea.BattleStatus.ACTIVE);
        ideaB.setBattleStatus(Idea.BattleStatus.ACTIVE);

        Battle battle = Battle.builder()
                .ideaA(ideaA)
                .ideaB(ideaB)
                .endsAt(LocalDateTime.now().plusHours(24))
                .status(Battle.BattleStatus.ACTIVE)
                .build();

        ideaRepository.save(ideaA);
        ideaRepository.save(ideaB);
        battle = battleRepository.save(battle);

        return mapToResponse(battle);
    }

    @Transactional(readOnly = true)
    public List<BattleResponse> getActiveBattles() {
        return battleRepository.findByStatus(Battle.BattleStatus.ACTIVE).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public LevelUpEvent voteInBattle(Long battleId, Long ideaId, Long userId) {
        Battle battle = battleRepository.findById(battleId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Battle not found"));

        if (battle.getStatus() != Battle.BattleStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Battle is not active");
        }

        if (!battle.getIdeaA().getId().equals(ideaId) && !battle.getIdeaB().getId().equals(ideaId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Idea is not part of this battle");
        }

        if (battleVoteRepository.existsByBattleIdAndUserId(battleId, userId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User has already voted in this battle");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        BattleVote vote = BattleVote.builder()
                .battle(battle)
                .user(user)
                .votedIdeaId(ideaId)
                .build();
        battleVoteRepository.save(vote);

        if (battle.getIdeaA().getId().equals(ideaId)) {
            battle.setVotesA(battle.getVotesA() + 1);
        } else {
            battle.setVotesB(battle.getVotesB() + 1);
        }

        battleRepository.save(battle);

        // Award BATTLE_VOTE XP and update streak for the voting user
        LevelUpEvent xpEvent     = xpService.awardXP(userId, "BATTLE_VOTE");
        LevelUpEvent streakEvent = userService.updateStreak(userId).orElse(null);

        broadcastBattleUpdate(battle);

        // Merge streak bonus into XP event and return for controller
        if (streakEvent != null) {
            return LevelUpEvent.builder()
                    .leveledUp(xpEvent.isLeveledUp() || streakEvent.isLeveledUp())
                    .newLevel(streakEvent.isLeveledUp() ? streakEvent.getNewLevel() : xpEvent.getNewLevel())
                    .xpGained(xpEvent.getXpGained() + streakEvent.getXpGained())
                    .totalXP(streakEvent.getTotalXP())
                    .build();
        }
        return xpEvent;
    }

    @Transactional(readOnly = true)
    public List<BattleResponse> getBattleHistory() {
        return battleRepository.findByStatus(Battle.BattleStatus.COMPLETED).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Scheduled(fixedRate = 60000)
    @Transactional
    public void resolveExpiredBattles() {
        List<Battle> expiredBattles = battleRepository.findExpiredActiveBattles(LocalDateTime.now());
        for (Battle battle : expiredBattles) {
            battle.setStatus(Battle.BattleStatus.COMPLETED);

            Idea ideaA = battle.getIdeaA();
            Idea ideaB = battle.getIdeaB();

            Idea winningIdea = null;
            if (battle.getVotesA() > battle.getVotesB()) {
                winningIdea = ideaA;
                ideaA.setBattleStatus(Idea.BattleStatus.WON);
                ideaB.setBattleStatus(Idea.BattleStatus.LOST);
                battle.setWinnerId(ideaA.getId());
            } else if (battle.getVotesB() > battle.getVotesA()) {
                winningIdea = ideaB;
                ideaB.setBattleStatus(Idea.BattleStatus.WON);
                ideaA.setBattleStatus(Idea.BattleStatus.LOST);
                battle.setWinnerId(ideaB.getId());
            } else {
                // Draw
                ideaA.setBattleStatus(Idea.BattleStatus.NONE);
                ideaB.setBattleStatus(Idea.BattleStatus.NONE);
            }

            ideaRepository.save(ideaA);
            ideaRepository.save(ideaB);
            battleRepository.save(battle);

            if (winningIdea != null) {
                Long ownerId = winningIdea.getUser().getId();
                xpService.awardXP(ownerId, "BATTLE_WON");
                badgeService.checkAndAwardBadges(ownerId);
            }

            broadcastBattleResolution(battle, winningIdea);
        }
    }

    private void broadcastBattleUpdate(Battle battle) {
        int totalVotes = battle.getVotesA() + battle.getVotesB();
        double percentA = totalVotes == 0 ? 0 : (double) battle.getVotesA() / totalVotes * 100;
        double percentB = totalVotes == 0 ? 0 : (double) battle.getVotesB() / totalVotes * 100;

        BattleWebSocketResponse wsResponse = BattleWebSocketResponse.builder()
                .votesA(battle.getVotesA())
                .votesB(battle.getVotesB())
                .percentA(percentA)
                .percentB(percentB)
                .totalVotes(totalVotes)
                .completed(false)
                .build();

        messagingTemplate.convertAndSend("/topic/battle/" + battle.getId(), wsResponse);
    }

    private void broadcastBattleResolution(Battle battle, Idea winningIdea) {
        int totalVotes = battle.getVotesA() + battle.getVotesB();
        double percentA = totalVotes == 0 ? 0 : (double) battle.getVotesA() / totalVotes * 100;
        double percentB = totalVotes == 0 ? 0 : (double) battle.getVotesB() / totalVotes * 100;

        BattleWebSocketResponse wsResponse = BattleWebSocketResponse.builder()
                .votesA(battle.getVotesA())
                .votesB(battle.getVotesB())
                .percentA(percentA)
                .percentB(percentB)
                .totalVotes(totalVotes)
                .completed(true)
                .winnerId(winningIdea != null ? winningIdea.getId() : null)
                .winnerTitle(winningIdea != null ? winningIdea.getTitle() : null)
                .build();

        messagingTemplate.convertAndSend("/topic/battle/" + battle.getId(), wsResponse);
    }

    private BattleResponse mapToResponse(Battle battle) {
        int totalVotes = battle.getVotesA() + battle.getVotesB();
        double percentA = totalVotes == 0 ? 0 : (double) battle.getVotesA() / totalVotes * 100;
        double percentB = totalVotes == 0 ? 0 : (double) battle.getVotesB() / totalVotes * 100;

        long timeRemainingSeconds = 0;
        if (battle.getStatus() == Battle.BattleStatus.ACTIVE && battle.getEndsAt() != null) {
            timeRemainingSeconds = Math.max(0, battle.getEndsAt().toEpochSecond(ZoneOffset.UTC) - LocalDateTime.now().toEpochSecond(ZoneOffset.UTC));
        }

        return BattleResponse.builder()
                .id(battle.getId())
                .ideaA(ideaService.mapToResponse(battle.getIdeaA()))
                .ideaB(ideaService.mapToResponse(battle.getIdeaB()))
                .votesA(battle.getVotesA())
                .votesB(battle.getVotesB())
                .percentA(percentA)
                .percentB(percentB)
                .timeRemainingSeconds(timeRemainingSeconds)
                .status(battle.getStatus().name())
                .winnerId(battle.getWinnerId())
                .build();
    }
}
