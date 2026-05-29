package com.ideaforge.ideaforge_backend.service;

import com.ideaforge.ideaforge_backend.dto.CollabRequestRequest;
import com.ideaforge.ideaforge_backend.dto.CollabRequestResponse;
import com.ideaforge.ideaforge_backend.dto.LevelUpEvent;
import com.ideaforge.ideaforge_backend.model.CollabRequest;
import com.ideaforge.ideaforge_backend.model.CollabRequest.CollabStatus;
import com.ideaforge.ideaforge_backend.model.Idea;
import com.ideaforge.ideaforge_backend.model.IdeaCollaborator;
import com.ideaforge.ideaforge_backend.model.User;
import com.ideaforge.ideaforge_backend.repository.CollabRequestRepository;
import com.ideaforge.ideaforge_backend.repository.IdeaCollaboratorRepository;
import com.ideaforge.ideaforge_backend.repository.IdeaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CollabRequestService {

    private final CollabRequestRepository collabRequestRepository;
    private final IdeaRepository ideaRepository;
    private final IdeaCollaboratorRepository ideaCollaboratorRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final XpService xpService;
    private final UserService userService;
    private final BadgeService badgeService;

    @Transactional
    public CollabRequestResponse createCollabRequest(Long ideaId, CollabRequestRequest request, User currentUser) {
        Idea idea = ideaRepository.findById(ideaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Idea not found"));

        if (idea.getUser().getId().equals(currentUser.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot send collab request to your own idea");
        }

        if (collabRequestRepository.existsByIdeaIdAndRequesterIdAndStatus(ideaId, currentUser.getId(), CollabStatus.PENDING)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You already have a pending request for this idea");
        }

        if (ideaCollaboratorRepository.existsByIdeaIdAndUserId(ideaId, currentUser.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You are already a collaborator on this idea");
        }

        CollabRequest collabRequest = CollabRequest.builder()
                .idea(idea)
                .requester(currentUser)
                .owner(idea.getUser())
                .message(request.getMessage())
                .status(CollabStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();

        CollabRequest savedRequest = collabRequestRepository.save(collabRequest);

        // Broadcast notification to idea owner
        Map<String, Object> notificationPayload = Map.of(
                "type", "COLLAB_REQUEST",
                "message", currentUser.getName() + " wants to collaborate on your idea '" + idea.getTitle() + "'",
                "requestId", savedRequest.getId()
        );
        messagingTemplate.convertAndSend("/topic/user/" + idea.getUser().getId() + "/notifications", notificationPayload);

        return mapToResponse(savedRequest);
    }

    public List<CollabRequestResponse> getReceivedRequests(User owner) {
        return collabRequestRepository.findByOwnerIdOrderByCreatedAtDesc(owner.getId()).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<CollabRequestResponse> getSentRequests(User requester) {
        return collabRequestRepository.findByRequesterIdOrderByCreatedAtDesc(requester.getId()).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public CollabRequestResponse acceptRequest(Long requestId, User currentUser) {
        CollabRequest request = getRequestForOwner(requestId, currentUser);

        if (request.getStatus() != CollabStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Request is already processed");
        }

        request.setStatus(CollabStatus.ACCEPTED);
        CollabRequest savedRequest = collabRequestRepository.save(request);

        IdeaCollaborator collaborator = IdeaCollaborator.builder()
                .idea(request.getIdea())
                .user(request.getRequester())
                .joinedAt(LocalDateTime.now())
                .build();
        ideaCollaboratorRepository.save(collaborator);

        // Award COLLAB_ACCEPTED XP (+75) to the requester
        Long requesterId = request.getRequester().getId();
        LevelUpEvent xpEvent     = xpService.awardXP(requesterId, "COLLAB_ACCEPTED");
        LevelUpEvent streakEvent = userService.updateStreak(requesterId).orElse(null);
        LevelUpEvent finalEvent  = mergeEvents(xpEvent, streakEvent);

        // Check COLLABORATOR badge for both requester and owner
        badgeService.checkAndAwardBadges(requesterId);
        badgeService.checkAndAwardBadges(request.getOwner().getId());

        CollabRequestResponse response = mapToResponse(savedRequest);
        response.setLevelUpEvent(finalEvent);
        return response;
    }

    @Transactional
    public CollabRequestResponse rejectRequest(Long requestId, User currentUser) {
        CollabRequest request = getRequestForOwner(requestId, currentUser);

        if (request.getStatus() != CollabStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Request is already processed");
        }

        request.setStatus(CollabStatus.REJECTED);
        CollabRequest savedRequest = collabRequestRepository.save(request);

        return mapToResponse(savedRequest);
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private CollabRequest getRequestForOwner(Long requestId, User currentUser) {
        CollabRequest request = collabRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Collaboration request not found"));

        if (!request.getOwner().getId().equals(currentUser.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not authorized to process this request");
        }
        return request;
    }

    private CollabRequestResponse mapToResponse(CollabRequest request) {
        return CollabRequestResponse.builder()
                .id(request.getId())
                .ideaId(request.getIdea().getId())
                .ideaTitle(request.getIdea().getTitle())
                .requesterId(request.getRequester().getId())
                .requesterName(request.getRequester().getName())
                .ownerId(request.getOwner().getId())
                .ownerName(request.getOwner().getName())
                .message(request.getMessage())
                .status(request.getStatus().name())
                .createdAt(request.getCreatedAt())
                .build();
    }

    private LevelUpEvent mergeEvents(LevelUpEvent primary, LevelUpEvent streak) {
        if (streak == null) return primary;
        boolean leveledUp = primary.isLeveledUp() || streak.isLeveledUp();
        String newLevel   = streak.isLeveledUp() ? streak.getNewLevel() : primary.getNewLevel();
        return LevelUpEvent.builder()
                .leveledUp(leveledUp)
                .newLevel(newLevel)
                .xpGained(primary.getXpGained() + streak.getXpGained())
                .totalXP(streak.getTotalXP())
                .build();
    }
}
