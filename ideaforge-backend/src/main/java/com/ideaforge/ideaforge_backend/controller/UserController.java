package com.ideaforge.ideaforge_backend.controller;

import com.ideaforge.ideaforge_backend.dto.BadgeResponse;
import com.ideaforge.ideaforge_backend.dto.IdeaResponse;
import com.ideaforge.ideaforge_backend.dto.UserProfileResponse;
import com.ideaforge.ideaforge_backend.model.User;
import com.ideaforge.ideaforge_backend.repository.BadgeRepository;
import com.ideaforge.ideaforge_backend.repository.UserRepository;
import com.ideaforge.ideaforge_backend.service.IdeaService;
import com.ideaforge.ideaforge_backend.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

/**
 * User profile and badge endpoints.
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService      userService;
    private final IdeaService      ideaService;
    private final BadgeRepository  badgeRepository;
    private final UserRepository   userRepository;

    /**
     * GET /api/users/me
     * Returns the authenticated user's full profile including XP, level, and streak_days.
     */
    @GetMapping("/me")
    public ResponseEntity<UserProfileResponse> getMyProfile(
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(userService.getProfile(currentUser));
    }

    /**
     * GET /api/users/{id}
     * Returns a public profile for any user. Authenticated callers only (so we can
     * show the correct context), but does NOT expose email for non-admins.
     */
    @GetMapping("/{id}")
    public ResponseEntity<UserProfileResponse> getUserById(@PathVariable Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        UserProfileResponse profile = userService.getProfile(user);
        // Strip email from public profile
        profile.setEmail(null);
        return ResponseEntity.ok(profile);
    }

    /**
     * GET /api/users/{id}/ideas
     * Returns all ideas posted by the specified user.
     */
    @GetMapping("/{id}/ideas")
    public ResponseEntity<List<IdeaResponse>> getIdeasByUser(@PathVariable Long id) {
        return ResponseEntity.ok(ideaService.getIdeasByUser(id));
    }

    /**
     * GET /api/users/{id}/collaborations
     * Ideas where this user is a collaborator (accepted), newest first.
     */
    @GetMapping("/{id}/collaborations")
    public ResponseEntity<List<IdeaResponse>> getCollaborationsByUser(@PathVariable Long id) {
        userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        return ResponseEntity.ok(ideaService.getCollaborationIdeasByUser(id));
    }

    /**
     * GET /api/users/{id}/badges
     * Returns all badges earned by the specified user, newest first.
     * Public endpoint — no authentication required.
     */
    @GetMapping("/{id}/badges")
    public ResponseEntity<List<BadgeResponse>> getBadges(@PathVariable Long id) {
        // Verify the user exists
        userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        List<BadgeResponse> badges = badgeRepository.findByUserIdOrderByEarnedAtDesc(id).stream()
                .map(badge -> BadgeResponse.builder()
                        .id(badge.getId())
                        .type(badge.getType())
                        .earnedAt(badge.getEarnedAt())
                        .build())
                .collect(Collectors.toList());

        return ResponseEntity.ok(badges);
    }
}
