package com.ideaforge.ideaforge_backend.controller;

import com.ideaforge.ideaforge_backend.dto.BattleCreateRequest;
import com.ideaforge.ideaforge_backend.dto.BattleResponse;
import com.ideaforge.ideaforge_backend.dto.BattleVoteRequest;
import com.ideaforge.ideaforge_backend.dto.LevelUpEvent;
import com.ideaforge.ideaforge_backend.model.User;
import com.ideaforge.ideaforge_backend.service.BattleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/battles")
@RequiredArgsConstructor
public class BattleController {

    private final BattleService battleService;

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<BattleResponse> createBattle(@RequestBody BattleCreateRequest request) {
        return ResponseEntity.ok(battleService.createBattle(request));
    }

    @GetMapping("/active")
    public ResponseEntity<List<BattleResponse>> getActiveBattles() {
        return ResponseEntity.ok(battleService.getActiveBattles());
    }

    /**
     * POST /api/battles/{id}/vote
     * Returns a LevelUpEvent for the voting user (BATTLE_VOTE XP + streak update).
     */
    @PostMapping("/{id}/vote")
    public ResponseEntity<LevelUpEvent> voteInBattle(
            @PathVariable Long id,
            @RequestBody BattleVoteRequest request,
            @AuthenticationPrincipal User currentUser) {
        LevelUpEvent event = battleService.voteInBattle(id, request.getIdeaId(), currentUser.getId());
        return ResponseEntity.ok(event);
    }

    @GetMapping("/history")
    public ResponseEntity<List<BattleResponse>> getBattleHistory() {
        return ResponseEntity.ok(battleService.getBattleHistory());
    }
}
