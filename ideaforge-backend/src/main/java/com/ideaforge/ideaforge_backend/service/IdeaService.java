package com.ideaforge.ideaforge_backend.service;

import com.ideaforge.ideaforge_backend.dto.CertificateResponse;
import com.ideaforge.ideaforge_backend.dto.DuplicateCheckRequest;
import com.ideaforge.ideaforge_backend.dto.DuplicateCheckResponse;
import com.ideaforge.ideaforge_backend.dto.IdeaCreateRequest;
import com.ideaforge.ideaforge_backend.dto.IdeaResponse;
import com.ideaforge.ideaforge_backend.dto.IdeaUpdateRequest;
import com.ideaforge.ideaforge_backend.dto.LevelUpEvent;
import com.ideaforge.ideaforge_backend.model.Idea;
import com.ideaforge.ideaforge_backend.model.IdeaCollaborator;
import com.ideaforge.ideaforge_backend.model.IdeaCertificate;
import com.ideaforge.ideaforge_backend.model.IdeaInvite;
import com.ideaforge.ideaforge_backend.model.User;
import com.ideaforge.ideaforge_backend.repository.IdeaCertificateRepository;
import com.ideaforge.ideaforge_backend.repository.IdeaInviteRepository;
import com.ideaforge.ideaforge_backend.repository.IdeaRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import com.ideaforge.ideaforge_backend.repository.IdeaCollaboratorRepository;

@Service
@RequiredArgsConstructor
public class IdeaService {

    private final IdeaRepository ideaRepository;
    private final IdeaCertificateRepository certificateRepository;
    private final IdeaInviteRepository ideaInviteRepository;
    private final IdeaCollaboratorRepository ideaCollaboratorRepository;
    private final TractionScoreService tractionScoreService;
    private final XpService xpService;
    private final UserService userService;
    private final BadgeService badgeService;
    private final IdeaHashService ideaHashService;
    @Lazy private final ViewLogService viewLogService;

    @Transactional
    public IdeaResponse createIdea(IdeaCreateRequest request, User currentUser) {
        if (!Boolean.TRUE.equals(request.getAgreedToTerms())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You must agree to the terms before posting");
        }

        Idea.Visibility vis = Idea.Visibility.PUBLIC;
        if (request.getVisibility() != null) {
            try {
                vis = Idea.Visibility.valueOf(request.getVisibility().toUpperCase());
            } catch (IllegalArgumentException e) {
                vis = Idea.Visibility.PUBLIC;
            }
        }

        Idea idea = Idea.builder()
                .title(request.getTitle())
                .pitch(request.getPitch())
                .problem(request.getProblem())
                .targetAudience(request.getTargetAudience())
                .category(request.getCategory())
                .visibility(vis)
                .agreedToTerms(true)
                .termsAgreedAt(LocalDateTime.now())
                .user(currentUser)
                .build();

        Idea savedIdea = ideaRepository.save(idea);

        // Auto-generate certificate
        IdeaCertificate certificate = IdeaCertificate.builder()
                .idea(savedIdea)
                .certificateCode(UUID.randomUUID().toString())
                .build();
        certificateRepository.save(certificate);

        // Layer 3 — generate and persist SHA-256 content hash
        ideaHashService.generateAndSaveHash(savedIdea);

        tractionScoreService.recalculateForIdea(savedIdea.getId());

        // Award IDEA_POSTED XP + update streak (merge into one event for the response)
        LevelUpEvent xpEvent = xpService.awardXP(currentUser.getId(), "IDEA_POSTED");
        LevelUpEvent streakEvent = userService.updateStreak(currentUser.getId()).orElse(null);
        LevelUpEvent finalEvent = mergeEvents(xpEvent, streakEvent);

        IdeaResponse response = mapToResponse(savedIdea);
        response.setLevelUpEvent(finalEvent);

        // Check FIRST_IDEA and SERIAL_PITCHER badges
        badgeService.checkAndAwardBadges(currentUser.getId());

        return response;
    }

    public Page<IdeaResponse> getIdeas(String category, Pageable pageable, User currentUser) {
        Page<Idea> ideas;
        if (category != null && !category.isBlank()) {
            ideas = ideaRepository.findByCategory(category, pageable);
        } else {
            ideas = ideaRepository.findAll(pageable);
        }

        List<IdeaResponse> filteredResponses = ideas.stream()
                .filter(idea -> {
                    if (idea.getVisibility() == Idea.Visibility.PUBLIC) return true;
                    if (currentUser == null) return false;
                    boolean isOwner = idea.getUser().getId().equals(currentUser.getId());
                    boolean isCollaborator = ideaCollaboratorRepository.existsByIdeaIdAndUserId(idea.getId(), currentUser.getId());
                    return isOwner || isCollaborator;
                })
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        return new PageImpl<>(filteredResponses, pageable, ideas.getTotalElements());
    }

    @Transactional
    public IdeaResponse getIdeaById(Long ideaId, String inviteToken, User currentUser,
                                     HttpServletRequest request) {
        Idea idea = ideaRepository.findById(ideaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Idea not found"));

        boolean isOwner = currentUser != null && idea.getUser().getId().equals(currentUser.getId());
        boolean isCollaborator = currentUser != null &&
                ideaCollaboratorRepository.existsByIdeaIdAndUserId(ideaId, currentUser.getId());

        if (idea.getVisibility() == Idea.Visibility.PUBLIC || isOwner || isCollaborator) {
            // Log the view (Layer 1) — skips if viewer is the owner
            if (request != null && currentUser != null && !isOwner) {
                viewLogService.logView(ideaId, currentUser, request);
            }
            IdeaResponse resp = mapToResponse(idea);
            // Layer 2 — set requiresConsent flag for non-owner viewers
            if (!isOwner && currentUser != null) {
                boolean alreadyConsented = viewLogService.hasConsented(ideaId, currentUser.getId());
                resp.setRequiresConsent(!alreadyConsented);
            }
            return resp;
        }

        if (idea.getVisibility() == Idea.Visibility.INVITE_ONLY) {
            if (inviteToken == null || inviteToken.isBlank()) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This idea is private. You need an invite link to view it.");
            }

            IdeaInvite invite = ideaInviteRepository.findByInviteToken(inviteToken)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid or expired invite token."));

            if (!invite.getIdea().getId().equals(ideaId)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Invite token does not match this idea.");
            }

            if (invite.getExpiresAt().isBefore(LocalDateTime.now())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Invite token has expired.");
            }

            if (!invite.getUsed()) {
                invite.setUsed(true);
                ideaInviteRepository.save(invite);
            }
            if (request != null && currentUser != null) {
                viewLogService.logView(ideaId, currentUser, request);
            }
            IdeaResponse resp = mapToResponse(idea);
            if (currentUser != null) {
                boolean alreadyConsented = viewLogService.hasConsented(ideaId, currentUser.getId());
                resp.setRequiresConsent(!alreadyConsented);
            }
            return resp;
        }

        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not have permission to view this idea.");
    }

    public List<String> getVisibilityOptions(Long ideaId, User currentUser) {
        Idea idea = ideaRepository.findById(ideaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Idea not found"));

        if (idea.getVisibility() == Idea.Visibility.PUBLIC) {
            return List.of("PUBLIC");
        } else {
            return List.of("PUBLIC", "PRIVATE", "INVITE_ONLY");
        }
    }

    @Transactional
    public IdeaResponse updateIdea(Long ideaId, IdeaUpdateRequest request, User currentUser) {
        Idea idea = ideaRepository.findById(ideaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Idea not found"));

        boolean isOwner = idea.getUser().getId().equals(currentUser.getId());
        boolean isCollaborator = ideaCollaboratorRepository.existsByIdeaIdAndUserId(idea.getId(), currentUser.getId());

        if (!isOwner && !isCollaborator) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the owner or a collaborator can edit this idea");
        }

        if (request.getTitle() != null) idea.setTitle(request.getTitle());
        if (request.getPitch() != null) idea.setPitch(request.getPitch());
        if (request.getProblem() != null) idea.setProblem(request.getProblem());
        if (request.getTargetAudience() != null) idea.setTargetAudience(request.getTargetAudience());
        if (request.getCategory() != null) idea.setCategory(request.getCategory());

        if (request.getVisibility() != null) {
            Idea.Visibility newVis = Idea.Visibility.valueOf(request.getVisibility().toUpperCase());
            if (idea.getVisibility() == Idea.Visibility.PUBLIC && newVis != Idea.Visibility.PUBLIC) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A PUBLIC idea cannot be made private again");
            }
            idea.setVisibility(newVis);
        }

        Idea updatedIdea = ideaRepository.save(idea);
        return mapToResponse(updatedIdea);
    }

    @Transactional
    public void deleteIdea(Long ideaId, User currentUser) {
        Idea idea = ideaRepository.findById(ideaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Idea not found"));

        if (!idea.getUser().getId().equals(currentUser.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the owner can delete this idea");
        }

        ideaRepository.delete(idea);
    }

    public CertificateResponse getCertificate(Long ideaId) {
        IdeaCertificate cert = certificateRepository.findByIdeaId(ideaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Certificate not found"));
        Idea idea = cert.getIdea();

        return CertificateResponse.builder()
                .certificateCode(cert.getCertificateCode())
                .ideaTitle(idea.getTitle())
                .ideaPitch(idea.getPitch())
                .ownerName(idea.getUser().getName())
                .issuedAt(idea.getCreatedAt())   // first-submission timestamp
                .ideaId(idea.getId())
                .build();
    }

    /** Returns all ideas posted by a user (for the profile page). */
    public List<IdeaResponse> getIdeasByUser(Long userId) {
        return ideaRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /** Ideas where the user is an accepted collaborator (not the primary owner). */
    public List<IdeaResponse> getCollaborationIdeasByUser(Long userId) {
        return ideaCollaboratorRepository.findByUserIdOrderByJoinedAtDesc(userId).stream()
                .map(IdeaCollaborator::getIdea)
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public IdeaResponse mapToResponse(Idea idea) {
        List<String> collabs = ideaCollaboratorRepository.findByIdeaId(idea.getId()).stream()
                .map(ic -> ic.getUser().getName())
                .collect(Collectors.toList());

        return IdeaResponse.builder()
                .id(idea.getId())
                .title(idea.getTitle())
                .pitch(idea.getPitch())
                .problem(idea.getProblem())
                .targetAudience(idea.getTargetAudience())
                .category(idea.getCategory())
                .userId(idea.getUser().getId())
                .authorName(idea.getUser().getName())
                .tractionScore(idea.getTractionScore())
                .voteCount(idea.getVoteCount())
                .commentCount(idea.getCommentCount())
                .battleStatus(idea.getBattleStatus().name())
                .visibility(idea.getVisibility().name())
                .isPlagiarised(idea.getIsPlagiarised())
                .createdAt(idea.getCreatedAt())
                .collaborators(collabs)
                .build();
    }

    /**
     * Checks whether the supplied title+pitch is similar to any existing PUBLIC idea.
     *
     * <p>Algorithm: Jaccard similarity on word-token sets, applied to the combined
     * "title + pitch" text of every public idea.  Threshold: 40 % → flag as duplicate.
     */
    @Transactional(readOnly = true)
    public DuplicateCheckResponse checkDuplicate(DuplicateCheckRequest request) {
        String candidateText = normalise(request.getTitle() + " " + request.getPitch());
        Set<String> candidateTokens = tokenSet(candidateText);

        if (candidateTokens.isEmpty()) {
            return DuplicateCheckResponse.builder().similarityFound(false).build();
        }

        // Compare against every public, non-plagiarised idea
        List<Idea> publicIdeas = ideaRepository.findAllPublicNonPlagiarised();

        DuplicateCheckResponse bestMatch = DuplicateCheckResponse.builder()
                .similarityFound(false).similarityScore(0).build();

        for (Idea idea : publicIdeas) {
            String existingText = normalise(idea.getTitle() + " " + idea.getPitch());
            int score = jaccardSimilarity(candidateTokens, tokenSet(existingText));
            if (score >= 40 && score > bestMatch.getSimilarityScore()) {
                bestMatch = DuplicateCheckResponse.builder()
                        .similarityFound(true)
                        .similarityScore(score)
                        .similarIdeaId(idea.getId())
                        .similarIdeaTitle(idea.getTitle())
                        .build();
            }
        }
        return bestMatch;
    }

    // ------------------------------------------------------------------
    // Private helpers
    // ------------------------------------------------------------------

    private String normalise(String text) {
        if (text == null) return "";
        return text.toLowerCase().replaceAll("[^a-z0-9 ]", " ").trim();
    }

    private Set<String> tokenSet(String text) {
        if (text.isBlank()) return new HashSet<>();
        return new HashSet<>(Arrays.asList(text.split("\\s+")));
    }

    /** Returns Jaccard similarity as an integer 0–100. */
    private int jaccardSimilarity(Set<String> a, Set<String> b) {
        if (a.isEmpty() || b.isEmpty()) return 0;
        Set<String> intersection = new HashSet<>(a);
        intersection.retainAll(b);
        Set<String> union = new HashSet<>(a);
        union.addAll(b);
        return (int) Math.round((double) intersection.size() / union.size() * 100);
    }

    /**
     * Merges two LevelUpEvents into one for the API response.
     * If a streak event fired (e.g. SEVEN_DAY_STREAK), its xpGained is added
     * on top of the primary event and leveledUp is ORed.
     */
    private LevelUpEvent mergeEvents(LevelUpEvent primary, LevelUpEvent streak) {
        if (streak == null) return primary;
        int totalXpGained = primary.getXpGained() + streak.getXpGained();
        boolean leveledUp  = primary.isLeveledUp() || streak.isLeveledUp();
        // Prefer the streak's newLevel if it leveled up (it reflects the latest state)
        String newLevel    = streak.isLeveledUp() ? streak.getNewLevel() : primary.getNewLevel();
        return LevelUpEvent.builder()
                .leveledUp(leveledUp)
                .newLevel(newLevel)
                .xpGained(totalXpGained)
                .totalXP(streak.getTotalXP())
                .build();
    }
}

