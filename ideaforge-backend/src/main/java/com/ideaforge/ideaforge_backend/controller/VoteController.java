package com.ideaforge.ideaforge_backend.controller;

import com.ideaforge.ideaforge_backend.dto.LevelUpEvent;
import com.ideaforge.ideaforge_backend.dto.VoteRequest;
import com.ideaforge.ideaforge_backend.model.User;
import com.ideaforge.ideaforge_backend.service.VoteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ideas/{id}/vote")
@RequiredArgsConstructor
public class VoteController {

    private final VoteService voteService;

    /**
     * POST /api/ideas/{id}/vote
     * Returns a LevelUpEvent for the voting user (streak update / level-up).
     * Note: the idea owner's RECEIVED_UPVOTE XP is handled internally and is
     * not included here — it can be surfaced via WebSocket if needed.
     */
    @PostMapping
    public ResponseEntity<LevelUpEvent> castVote(
            @PathVariable Long id,
            @Valid @RequestBody VoteRequest request,
            @AuthenticationPrincipal User currentUser) {
        LevelUpEvent event = voteService.castVote(id, request, currentUser);
        return ResponseEntity.ok(event);
    }

    @GetMapping
    public ResponseEntity<String> getUserVote(
            @PathVariable Long id,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(voteService.getUserVoteType(id, currentUser));
    }
}
