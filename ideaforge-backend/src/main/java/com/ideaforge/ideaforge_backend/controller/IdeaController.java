package com.ideaforge.ideaforge_backend.controller;

import com.ideaforge.ideaforge_backend.dto.*;
import com.ideaforge.ideaforge_backend.dto.DuplicateCheckRequest;
import com.ideaforge.ideaforge_backend.dto.DuplicateCheckResponse;
import com.ideaforge.ideaforge_backend.model.User;
import com.ideaforge.ideaforge_backend.service.IdeaInviteService;
import com.ideaforge.ideaforge_backend.service.IdeaService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ideas")
@RequiredArgsConstructor
public class IdeaController {

    private final IdeaService ideaService;
    private final IdeaInviteService ideaInviteService;

    @PostMapping
    public ResponseEntity<IdeaResponse> createIdea(
            @Valid @RequestBody IdeaCreateRequest request,
            @AuthenticationPrincipal User currentUser) {
        IdeaResponse response = ideaService.createIdea(request, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * POST /api/ideas/check-duplicate
     * Checks similarity of a proposed idea title+pitch against all existing public ideas.
     * Returns a DuplicateCheckResponse with similarityFound, similarityScore, and best-match info.
     */
    @PostMapping("/check-duplicate")
    public ResponseEntity<DuplicateCheckResponse> checkDuplicate(
            @RequestBody DuplicateCheckRequest request,
            @AuthenticationPrincipal User currentUser) {
        DuplicateCheckResponse response = ideaService.checkDuplicate(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<Page<IdeaResponse>> getIdeas(
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "trending") String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @AuthenticationPrincipal User currentUser) {

        Sort sortObj;
        if ("newest".equalsIgnoreCase(sort)) {
            sortObj = Sort.by(Sort.Direction.DESC, "createdAt");
        } else {
            sortObj = Sort.by(Sort.Direction.DESC, "tractionScore");
        }

        Pageable pageable = PageRequest.of(page, size, sortObj);
        Page<IdeaResponse> responses = ideaService.getIdeas(category, pageable, currentUser);
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/{id}")
    public ResponseEntity<IdeaResponse> getIdeaById(
            @PathVariable Long id,
            @RequestParam(required = false) String inviteToken,
            @AuthenticationPrincipal User currentUser,
            HttpServletRequest request) {
        IdeaResponse response = ideaService.getIdeaById(id, inviteToken, currentUser, request);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<IdeaResponse> updateIdea(
            @PathVariable Long id,
            @RequestBody IdeaUpdateRequest request,
            @AuthenticationPrincipal User currentUser) {
        IdeaResponse response = ideaService.updateIdea(id, request, currentUser);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteIdea(
            @PathVariable Long id,
            @AuthenticationPrincipal User currentUser) {
        ideaService.deleteIdea(id, currentUser);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/certificate")
    public ResponseEntity<CertificateResponse> getCertificate(@PathVariable Long id) {
        CertificateResponse response = ideaService.getCertificate(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/visibility-options")
    public ResponseEntity<List<String>> getVisibilityOptions(
            @PathVariable Long id,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(ideaService.getVisibilityOptions(id, currentUser));
    }

    @PostMapping("/{id}/invites")
    public ResponseEntity<InviteCreateResponse> createInvite(
            @PathVariable Long id,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ideaInviteService.createInvite(id, currentUser));
    }

    @GetMapping("/{id}/invites")
    public ResponseEntity<List<InviteResponse>> getInvites(
            @PathVariable Long id,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(ideaInviteService.getInvites(id, currentUser));
    }

    @DeleteMapping("/{id}/invites/{inviteId}")
    public ResponseEntity<Void> revokeInvite(
            @PathVariable Long id,
            @PathVariable Long inviteId,
            @AuthenticationPrincipal User currentUser) {
        ideaInviteService.revokeInvite(id, inviteId, currentUser);
        return ResponseEntity.noContent().build();
    }
}
