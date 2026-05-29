package com.ideaforge.ideaforge_backend.service;

import com.ideaforge.ideaforge_backend.dto.InviteCreateResponse;
import com.ideaforge.ideaforge_backend.dto.InviteResponse;
import com.ideaforge.ideaforge_backend.model.Idea;
import com.ideaforge.ideaforge_backend.model.IdeaInvite;
import com.ideaforge.ideaforge_backend.model.User;
import com.ideaforge.ideaforge_backend.repository.IdeaInviteRepository;
import com.ideaforge.ideaforge_backend.repository.IdeaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class IdeaInviteService {

    private final IdeaInviteRepository inviteRepository;
    private final IdeaRepository ideaRepository;

    @Transactional
    public InviteCreateResponse createInvite(Long ideaId, User currentUser) {
        Idea idea = ideaRepository.findById(ideaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Idea not found"));

        if (!idea.getUser().getId().equals(currentUser.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the owner can generate invites");
        }

        if (idea.getVisibility() != Idea.Visibility.INVITE_ONLY) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invites can only be generated for INVITE_ONLY ideas");
        }

        String token = UUID.randomUUID().toString();
        LocalDateTime expiresAt = LocalDateTime.now().plusDays(7);

        IdeaInvite invite = IdeaInvite.builder()
                .idea(idea)
                .invitedBy(currentUser)
                .inviteToken(token)
                .expiresAt(expiresAt)
                .used(false)
                .createdAt(LocalDateTime.now())
                .build();

        inviteRepository.save(invite);

        return InviteCreateResponse.builder()
                .inviteToken(token)
                .shareableLink("/ideas/" + ideaId + "?token=" + token)
                .expiresAt(expiresAt.toString())
                .build();
    }

    public List<InviteResponse> getInvites(Long ideaId, User currentUser) {
        Idea idea = ideaRepository.findById(ideaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Idea not found"));

        if (!idea.getUser().getId().equals(currentUser.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the owner can view invites");
        }

        return inviteRepository.findByIdeaIdOrderByCreatedAtDesc(ideaId).stream()
                .map(invite -> InviteResponse.builder()
                        .id(invite.getId())
                        .inviteToken(invite.getInviteToken())
                        .expiresAt(invite.getExpiresAt())
                        .used(invite.getUsed())
                        .createdAt(invite.getCreatedAt())
                        .build())
                .collect(Collectors.toList());
    }

    @Transactional
    public void revokeInvite(Long ideaId, Long inviteId, User currentUser) {
        Idea idea = ideaRepository.findById(ideaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Idea not found"));

        if (!idea.getUser().getId().equals(currentUser.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the owner can revoke invites");
        }

        IdeaInvite invite = inviteRepository.findById(inviteId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Invite not found"));

        if (!invite.getIdea().getId().equals(ideaId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invite does not belong to this idea");
        }

        inviteRepository.delete(invite);
    }
}
