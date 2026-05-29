package com.ideaforge.ideaforge_backend.service;

import com.ideaforge.ideaforge_backend.model.Idea;
import com.ideaforge.ideaforge_backend.model.IdeaHash;
import com.ideaforge.ideaforge_backend.repository.IdeaHashRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Layer 3 — SHA-256 Idea Hash
 *
 * Generates and stores an immutable SHA-256 hash of every idea at the moment it
 * is posted.  The hash proves the idea content has not been altered since submission
 * and can serve as evidence in an IP-dispute scenario.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IdeaHashService {

    private final IdeaHashRepository ideaHashRepository;

    /**
     * Builds the canonical hash input string, computes SHA-256, and persists the record.
     * Called once immediately after a new idea is saved.
     *
     * @param idea the newly persisted {@link Idea}
     */
    @Transactional
    public void generateAndSaveHash(Idea idea) {
        if (ideaHashRepository.existsByIdeaId(idea.getId())) {
            // Idempotent — skip if already generated (should never happen in normal flow)
            return;
        }

        // Canonical input: fixed-separator concatenation of immutable idea fields
        String hashInput = idea.getTitle()
                + "|" + (idea.getPitch() != null ? idea.getPitch() : "")
                + "|" + idea.getUser().getId()
                + "|" + idea.getCreatedAt().toString();

        String sha256Hex = sha256Hex(hashInput);

        IdeaHash hash = IdeaHash.builder()
                .idea(idea)
                .sha256Hash(sha256Hex)
                .hashInput(hashInput)
                .build();

        ideaHashRepository.save(hash);
        log.info("[IdeaHash] Generated hash {} for idea {}", sha256Hex.substring(0, 8) + "...", idea.getId());
    }

    /**
     * Returns the stored hash for an idea, or {@code null} if not yet generated.
     * Used by the public GET /api/ideas/{id}/hash endpoint.
     */
    public IdeaHash getHash(Long ideaId) {
        return ideaHashRepository.findByIdeaId(ideaId).orElse(null);
    }

    // ------------------------------------------------------------------
    // Private helpers
    // ------------------------------------------------------------------

    private static String sha256Hex(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(64);
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 is always available on the JVM — this is unreachable
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }
}
