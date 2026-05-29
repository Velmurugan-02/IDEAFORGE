package com.ideaforge.ideaforge_backend.service;

import com.ideaforge.ideaforge_backend.model.Idea;
import com.ideaforge.ideaforge_backend.repository.IdeaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class LiveLeaderboardService {

    private final IdeaRepository ideaRepository;
    private final SimpMessagingTemplate messagingTemplate;

    // Stores previous rankings by Idea ID -> Rank (1-based)
    private Map<Long, Integer> previousRankings = new HashMap<>();

    @Transactional(readOnly = true)
    @Scheduled(fixedRate = 30000)
    public void broadcastLeaderboard() {
        List<Idea> topIdeas = ideaRepository.findByVisibilityAndIsPlagiarisedFalseOrderByTractionScoreDesc(
                Idea.Visibility.PUBLIC, PageRequest.of(0, 10)).getContent();

        List<Map<String, Object>> leaderboardPayload = new ArrayList<>();
        Map<Long, Integer> currentRankings = new HashMap<>();

        for (int i = 0; i < topIdeas.size(); i++) {
            Idea idea = topIdeas.get(i);
            int currentRank = i + 1;
            currentRankings.put(idea.getId(), currentRank);

            String rankChange = "NEW";
            if (previousRankings.containsKey(idea.getId())) {
                int previousRank = previousRankings.get(idea.getId());
                if (currentRank < previousRank) {
                    rankChange = "UP";
                } else if (currentRank > previousRank) {
                    rankChange = "DOWN";
                } else {
                    rankChange = "SAME";
                }
            }

            leaderboardPayload.add(Map.of(
                    "rank", currentRank,
                    "ideaId", idea.getId(),
                    "title", idea.getTitle(),
                    "ownerName", idea.getUser().getName(),
                    "tractionScore", idea.getTractionScore(),
                    "voteCount", idea.getVoteCount(),
                    "rankChange", rankChange
            ));
        }

        // Broadcast to leaderboard topic
        messagingTemplate.convertAndSend("/topic/leaderboard", leaderboardPayload);

        // Update previous rankings for next tick
        previousRankings = currentRankings;
    }
}
