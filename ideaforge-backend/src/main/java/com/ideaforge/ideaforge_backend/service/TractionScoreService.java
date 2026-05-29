package com.ideaforge.ideaforge_backend.service;

import com.ideaforge.ideaforge_backend.model.Idea;
import com.ideaforge.ideaforge_backend.model.Comment;
import com.ideaforge.ideaforge_backend.repository.CommentRepository;
import com.ideaforge.ideaforge_backend.repository.IdeaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class TractionScoreService {

    private final IdeaRepository ideaRepository;
    private final CommentRepository commentRepository;

    @Transactional
    public void recalculateForIdea(Long ideaId) {
        ideaRepository.findById(ideaId).ifPresent(this::calculateAndSaveScore);
    }

    // Run every hour
    @Scheduled(fixedRate = 3600000)
    @Transactional
    public void recalculateAllPublicIdeas() {
        log.info("Starting hourly recalculation of traction scores for all PUBLIC ideas...");
        int page = 0;
        int size = 100;
        Page<Idea> ideasPage;

        do {
            ideasPage = ideaRepository.findByVisibility(
                    Idea.Visibility.PUBLIC, 
                    PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "id"))
            );
            for (Idea idea : ideasPage.getContent()) {
                calculateAndSaveScore(idea);
            }
            page++;
        } while (ideasPage.hasNext());
        
        log.info("Finished traction score recalculation.");
    }

    private void calculateAndSaveScore(Idea idea) {
        long hoursSincePosted = Duration.between(idea.getCreatedAt(), LocalDateTime.now()).toHours();
        if (hoursSincePosted < 0) hoursSincePosted = 0;

        long unansweredChallenges = commentRepository.countByIdeaIdAndTagAndAnsweredFalse(idea.getId(), Comment.CommentTag.CHALLENGE);

        double score = (idea.getVoteCount() * 3.0) + (idea.getCommentCount() * 2.0) - (hoursSincePosted * 0.1) - (unansweredChallenges * 5.0);
        if (score < 0) {
            score = 0;
        }

        idea.setTractionScore(score);
        ideaRepository.save(idea);
    }
}
