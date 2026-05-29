package com.ideaforge.ideaforge_backend.controller;

import com.ideaforge.ideaforge_backend.dto.IdeaHashResponse;
import com.ideaforge.ideaforge_backend.dto.ViewLogResponse;
import com.ideaforge.ideaforge_backend.dto.ViewStatsResponse;
import com.ideaforge.ideaforge_backend.model.IdeaHash;
import com.ideaforge.ideaforge_backend.model.User;
import com.ideaforge.ideaforge_backend.service.IdeaHashService;
import com.ideaforge.ideaforge_backend.service.ViewLogService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

/**
 * Endpoints for the Silent Idea Theft Prevention System:
 *
 * Layer 1 + 2: POST /api/ideas/{id}/consent           (authenticated user)
 * Layer 3:     GET  /api/ideas/{id}/hash              (public)
 * Layer 4:     GET  /api/ideas/{id}/view-stats        (idea owner)
 * Admin:       GET  /api/admin/ideas/{id}/view-logs   (ROLE_ADMIN)
 */
@RestController
@RequiredArgsConstructor
public class ViewLogController {

    private final ViewLogService  viewLogService;
    private final IdeaHashService ideaHashService;

    // ------------------------------------------------------------------
    // Layer 2 — Consent Gate
    // ------------------------------------------------------------------

    /**
     * POST /api/ideas/{id}/consent
     * Marks that the authenticated viewer has explicitly acknowledged
     * they are viewing a protected idea.  Called by the frontend consent popup.
     */
    @PostMapping("/api/ideas/{id}/consent")
    public ResponseEntity<Map<String, Object>> recordConsent(
            @PathVariable Long id,
            @AuthenticationPrincipal User currentUser,
            HttpServletRequest request) {

        if (currentUser == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Must be logged in to consent");
        }
        Map<String, Object> result = viewLogService.recordConsent(id, currentUser);
        return ResponseEntity.ok(result);
    }

    // ------------------------------------------------------------------
    // Layer 3 — SHA-256 Idea Hash (public)
    // ------------------------------------------------------------------

    /**
     * GET /api/ideas/{id}/hash
     * Returns the immutable SHA-256 hash generated at the moment the idea was posted.
     * Public — no authentication required.
     */
    @GetMapping("/api/ideas/{id}/hash")
    public ResponseEntity<IdeaHashResponse> getIdeaHash(@PathVariable Long id) {
        IdeaHash hash = ideaHashService.getHash(id);
        if (hash == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Hash not found for this idea. It may have been posted before this feature was enabled.");
        }
        IdeaHashResponse response = IdeaHashResponse.builder()
                .ideaId(id)
                .sha256Hash(hash.getSha256Hash())
                .generatedAt(hash.getGeneratedAt())
                .verificationNote("This hash was generated at the moment of posting and cannot be altered.")
                .build();
        return ResponseEntity.ok(response);
    }

    // ------------------------------------------------------------------
    // Layer 4 — View Statistics (owner only)
    // ------------------------------------------------------------------

    /**
     * GET /api/ideas/{id}/view-stats
     * Returns aggregate view data for the idea owner's dashboard.
     */
    @GetMapping("/api/ideas/{id}/view-stats")
    public ResponseEntity<ViewStatsResponse> getViewStats(
            @PathVariable Long id,
            @AuthenticationPrincipal User currentUser) {

        if (currentUser == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        ViewStatsResponse stats = viewLogService.getViewStats(id, currentUser);
        return ResponseEntity.ok(stats);
    }

    // ------------------------------------------------------------------
    // Admin — Evidence view logs
    // ------------------------------------------------------------------

    /**
     * GET /api/admin/ideas/{id}/view-logs
     * Returns all view logs for an idea with masked PII — for admin/evidence use only.
     */
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/api/admin/ideas/{id}/view-logs")
    public ResponseEntity<List<ViewLogResponse>> getViewLogs(@PathVariable Long id) {
        List<ViewLogResponse> logs = viewLogService.getViewLogsForIdea(id);
        return ResponseEntity.ok(logs);
    }
}
