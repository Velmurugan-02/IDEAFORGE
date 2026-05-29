package com.ideaforge.ideaforge_backend.service;

import com.ideaforge.ideaforge_backend.dto.CommentRequest;
import com.ideaforge.ideaforge_backend.dto.CommentResponse;
import com.ideaforge.ideaforge_backend.dto.LevelUpEvent;
import com.ideaforge.ideaforge_backend.model.Comment;
import com.ideaforge.ideaforge_backend.model.Idea;
import com.ideaforge.ideaforge_backend.model.User;
import com.ideaforge.ideaforge_backend.repository.CommentRepository;
import com.ideaforge.ideaforge_backend.repository.IdeaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;
    private final IdeaRepository ideaRepository;
    private final TractionScoreService tractionScoreService;
    private final XpService xpService;
    private final BadgeService badgeService;
    private final UserService userService;

    /**
     * Posts a comment and awards COMMENT_POSTED XP.
     * The LevelUpEvent (possibly merged with a 7-day streak bonus) is embedded
     * in the returned CommentResponse for the frontend toast.
     */
    @Transactional
    public CommentResponse postComment(Long ideaId, CommentRequest request, User currentUser) {
        Idea idea = ideaRepository.findById(ideaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Idea not found"));

        Comment.CommentTag tag;
        try {
            tag = Comment.CommentTag.valueOf(request.getTag().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid comment tag");
        }

        Comment comment = Comment.builder()
                .idea(idea)
                .user(currentUser)
                .content(request.getContent())
                .tag(tag)
                .answered(false)
                .createdAt(LocalDateTime.now())
                .build();

        Comment savedComment = commentRepository.save(comment);

        idea.setCommentCount(idea.getCommentCount() + 1);
        ideaRepository.save(idea);

        tractionScoreService.recalculateForIdea(ideaId);

        // Award XP + update streak; merge if streak bonus fired
        LevelUpEvent xpEvent     = xpService.awardXP(currentUser.getId(), "COMMENT_POSTED");
        LevelUpEvent streakEvent = userService.updateStreak(currentUser.getId()).orElse(null);
        LevelUpEvent finalEvent  = mergeEvents(xpEvent, streakEvent);

        CommentResponse response = mapToResponse(savedComment);
        response.setLevelUpEvent(finalEvent);
        return response;
    }

    public List<CommentResponse> getComments(Long ideaId, String tagFilter) {
        if (tagFilter != null && !tagFilter.isEmpty()) {
            try {
                Comment.CommentTag tag = Comment.CommentTag.valueOf(tagFilter.toUpperCase());
                return commentRepository.findByIdeaIdAndTagOrderByCreatedAtDesc(ideaId, tag).stream()
                        .map(this::mapToResponse)
                        .collect(Collectors.toList());
            } catch (IllegalArgumentException e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid tag filter");
            }
        } else {
            return commentRepository.findByIdeaIdOrderByCreatedAtDesc(ideaId, PageRequest.of(0, 100)).stream()
                    .map(this::mapToResponse)
                    .collect(Collectors.toList());
        }
    }

    /**
     * Marks a comment as answered (CHALLENGE tag). Awards CHALLENGE_ANSWERED XP
     * and embeds the LevelUpEvent in the response.
     */
    @Transactional
    public CommentResponse answerComment(Long commentId, User currentUser) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Comment not found"));

        if (!comment.getIdea().getUser().getId().equals(currentUser.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the idea owner can answer comments");
        }

        if (Boolean.TRUE.equals(comment.getAnswered())) {
            return mapToResponse(comment);
        }

        comment.setAnswered(true);
        Comment savedComment = commentRepository.save(comment);

        LevelUpEvent finalEvent = null;
        if (comment.getTag() == Comment.CommentTag.CHALLENGE) {
            LevelUpEvent xpEvent     = xpService.awardXP(currentUser.getId(), "CHALLENGE_ANSWERED");
            LevelUpEvent streakEvent = userService.updateStreak(currentUser.getId()).orElse(null);
            finalEvent               = mergeEvents(xpEvent, streakEvent);
            badgeService.checkAndAwardBadges(currentUser.getId()); // GHOST_BUSTER check
        }

        tractionScoreService.recalculateForIdea(comment.getIdea().getId());

        CommentResponse response = mapToResponse(savedComment);
        response.setLevelUpEvent(finalEvent);
        return response;
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private CommentResponse mapToResponse(Comment comment) {
        return CommentResponse.builder()
                .id(comment.getId())
                .ideaId(comment.getIdea().getId())
                .userId(comment.getUser().getId())
                .userName(comment.getUser().getName())
                .content(comment.getContent())
                .tag(comment.getTag().name())
                .answered(comment.getAnswered())
                .createdAt(comment.getCreatedAt())
                .build();
    }

    private LevelUpEvent mergeEvents(LevelUpEvent primary, LevelUpEvent streak) {
        if (streak == null) return primary;
        int totalXpGained = primary.getXpGained() + streak.getXpGained();
        boolean leveledUp = primary.isLeveledUp() || streak.isLeveledUp();
        String newLevel   = streak.isLeveledUp() ? streak.getNewLevel() : primary.getNewLevel();
        return LevelUpEvent.builder()
                .leveledUp(leveledUp)
                .newLevel(newLevel)
                .xpGained(totalXpGained)
                .totalXP(streak.getTotalXP())
                .build();
    }
}
