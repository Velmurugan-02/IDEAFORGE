package com.ideaforge.ideaforge_backend.controller;

import com.ideaforge.ideaforge_backend.dto.HallOfFameResponse;
import com.ideaforge.ideaforge_backend.service.HallOfFameService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Public endpoint for the Hall of Fame — no authentication required.
 */
@RestController
@RequestMapping("/api/hall-of-fame")
@RequiredArgsConstructor
public class HallOfFameController {

    private final HallOfFameService hallOfFameService;

    /**
     * GET /api/hall-of-fame
     * Returns all Hall of Fame entries ordered by week descending, rank ascending.
     * The frontend groups them by {@code weekStart} to display weekly leaderboards.
     */
    @GetMapping
    public ResponseEntity<List<HallOfFameResponse>> getHallOfFame() {
        return ResponseEntity.ok(hallOfFameService.getAllEntries());
    }
}
