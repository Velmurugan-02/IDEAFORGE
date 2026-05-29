package com.ideaforge.ideaforge_backend.controller;

import com.ideaforge.ideaforge_backend.dto.KeywordAlertResponse;
import com.ideaforge.ideaforge_backend.dto.KeywordWatchRequest;
import com.ideaforge.ideaforge_backend.dto.KeywordWatchResponse;
import com.ideaforge.ideaforge_backend.model.User;
import com.ideaforge.ideaforge_backend.service.KeywordWatchService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Layer 5 — Keyword Watch REST API
 *
 * POST   /api/ideas/{id}/keywords                  — add keyword (owner only, max 5)
 * GET    /api/ideas/{id}/keywords                  — list keywords for idea (owner only)
 * DELETE /api/ideas/{id}/keywords/{keywordId}      — remove keyword (owner only)
 * GET    /api/users/me/keyword-alerts              — get & mark-read all alerts for current user
 */
@RestController
@RequiredArgsConstructor
public class KeywordWatchController {

    private final KeywordWatchService keywordWatchService;

    @PostMapping("/api/ideas/{id}/keywords")
    public ResponseEntity<KeywordWatchResponse> addKeyword(
            @PathVariable Long id,
            @Valid @RequestBody KeywordWatchRequest request,
            @AuthenticationPrincipal User currentUser) {

        KeywordWatchResponse response = keywordWatchService.addKeyword(id, request, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/api/ideas/{id}/keywords")
    public ResponseEntity<List<KeywordWatchResponse>> listKeywords(
            @PathVariable Long id,
            @AuthenticationPrincipal User currentUser) {

        return ResponseEntity.ok(keywordWatchService.listKeywords(id, currentUser));
    }

    @DeleteMapping("/api/ideas/{id}/keywords/{keywordId}")
    public ResponseEntity<Void> removeKeyword(
            @PathVariable Long id,
            @PathVariable Long keywordId,
            @AuthenticationPrincipal User currentUser) {

        keywordWatchService.removeKeyword(id, keywordId, currentUser);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/api/users/me/keyword-alerts")
    public ResponseEntity<List<KeywordAlertResponse>> getMyAlerts(
            @AuthenticationPrincipal User currentUser) {

        return ResponseEntity.ok(keywordWatchService.getMyAlerts(currentUser));
    }
}
