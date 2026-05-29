package com.ideaforge.ideaforge_backend.service;

import com.ideaforge.ideaforge_backend.dto.ViewLogResponse;
import com.ideaforge.ideaforge_backend.dto.ViewStatsResponse;
import com.ideaforge.ideaforge_backend.model.Idea;
import com.ideaforge.ideaforge_backend.model.User;
import com.ideaforge.ideaforge_backend.model.ViewLog;
import com.ideaforge.ideaforge_backend.repository.IdeaRepository;
import com.ideaforge.ideaforge_backend.repository.UserRepository;
import com.ideaforge.ideaforge_backend.repository.ViewLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Layer 1 + 4 — View Tracking Log + Owner View Notifications
 *
 * Responsibilities:
 *  - Log every unique daily view per (idea, viewer) pair
 *  - Extract IP and device fingerprint from the HTTP request
 *  - Fire WebSocket notifications to the idea owner on new unique daily views
 *  - Provide masked view-log data for admin evidence review
 *  - Serve view statistics to the idea owner
 *  - Handle the consent endpoint
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ViewLogService {

    private final ViewLogRepository     viewLogRepository;
    private final IdeaRepository        ideaRepository;
    private final UserRepository        userRepository;
    private final SimpMessagingTemplate messagingTemplate;

    // ------------------------------------------------------------------
    // Layer 1 — Log view
    // ------------------------------------------------------------------

    /**
     * Logs or refreshes a view entry for today.
     * <ul>
     *   <li>If no entry exists for (idea, viewer, today): creates a new record and
     *       fires a WebSocket notification to the owner.</li>
     *   <li>If an entry already exists today: updates {@code viewedAt} to now.</li>
     * </ul>
     *
     * @param ideaId    the idea being viewed
     * @param viewer    the authenticated viewer (may be {@code null} for unauthenticated requests)
     * @param request   the HTTP request (for IP + User-Agent extraction)
     */
    @Transactional
    public void logView(Long ideaId, User viewer, HttpServletRequest request) {
        if (viewer == null) return; // only log authenticated views

        Idea idea = ideaRepository.findById(ideaId).orElse(null);
        if (idea == null) return;

        // Do not log the owner viewing their own idea
        if (idea.getUser().getId().equals(viewer.getId())) return;

        LocalDateTime dayStart = LocalDate.now().atStartOfDay();
        Optional<ViewLog> existing = viewLogRepository.findTodayEntry(ideaId, viewer.getId(), dayStart);

        if (existing.isPresent()) {
            // Refresh the timestamp for the existing today-entry
            ViewLog log = existing.get();
            log.setViewedAt(LocalDateTime.now());
            viewLogRepository.save(log);
        } else {
            // New unique daily view — create entry and notify the owner
            String ip          = extractIp(request);
            String fingerprint = fingerprintUserAgent(request.getHeader("User-Agent"));

            ViewLog newLog = ViewLog.builder()
                    .idea(idea)
                    .viewer(viewer)
                    .ipAddress(ip)
                    .deviceFingerprint(fingerprint)
                    .build();
            viewLogRepository.save(newLog);

            // Fire Layer-4 WebSocket notification
            sendOwnerNotification(idea, viewer);
        }
    }

    // ------------------------------------------------------------------
    // Layer 2 — Consent
    // ------------------------------------------------------------------

    /**
     * Finds the most-recent view log for the viewer on this idea today and
     * marks it as consented.  Returns the updated record as a simple map.
     */
    @Transactional
    public Map<String, Object> recordConsent(Long ideaId, User viewer) {
        LocalDateTime dayStart = LocalDate.now().atStartOfDay();
        ViewLog viewLog = viewLogRepository.findTodayEntry(ideaId, viewer.getId(), dayStart)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "No view record found for today. Please visit the idea page first."));

        LocalDateTime now = LocalDateTime.now();
        viewLog.setConsentGiven(true);
        viewLog.setConsentGivenAt(now);
        viewLogRepository.save(viewLog);

        Map<String, Object> result = new HashMap<>();
        result.put("consentRecorded", true);
        result.put("timestamp", now.toString());
        return result;
    }

    /**
     * Returns whether this viewer has ever consented for this idea
     * (used to set {@code requiresConsent} on the IdeaResponse).
     */
    public boolean hasConsented(Long ideaId, Long viewerId) {
        return viewLogRepository.hasConsented(ideaId, viewerId);
    }

    // ------------------------------------------------------------------
    // Admin — Evidence view logs (masked)
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<ViewLogResponse> getViewLogsForIdea(Long ideaId) {
        return viewLogRepository.findByIdeaIdOrderByViewedAtDesc(ideaId).stream()
                .map(this::toMaskedResponse)
                .collect(Collectors.toList());
    }

    // ------------------------------------------------------------------
    // View statistics (owner only)
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public ViewStatsResponse getViewStats(Long ideaId, User owner) {
        Idea idea = ideaRepository.findById(ideaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Idea not found"));

        if (!idea.getUser().getId().equals(owner.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the idea owner can view stats");
        }

        LocalDateTime dayStart  = LocalDate.now().atStartOfDay();
        LocalDateTime weekStart = dayStart.minusDays(6);

        return ViewStatsResponse.builder()
                .totalViews((int)    viewLogRepository.countByIdeaId(ideaId))
                .uniqueViewers((int) viewLogRepository.countUniqueViewers(ideaId))
                .viewsToday((int)    viewLogRepository.countViewsToday(ideaId, dayStart))
                .viewsThisWeek((int) viewLogRepository.countViewsThisWeek(ideaId, weekStart))
                .consentedViewers((int) viewLogRepository.countConsentedViewers(ideaId))
                .build();
    }

    // ------------------------------------------------------------------
    // Layer 4 — WebSocket notification helpers
    // ------------------------------------------------------------------

    private void sendOwnerNotification(Idea idea, User viewer) {
        try {
            long totalViews   = viewLogRepository.countByIdeaId(idea.getId());
            Long ownerId      = idea.getUser().getId();

            Map<String, Object> payload = new HashMap<>();
            payload.put("type",       "NEW_VIEW");
            payload.put("message",    "Someone new viewed your idea '" + idea.getTitle() + "'");
            payload.put("totalViews", totalViews);
            payload.put("viewedAt",   LocalDateTime.now().toString());
            // Deliberately omit viewer identity

            messagingTemplate.convertAndSend(
                    "/topic/user/" + ownerId + "/notifications", payload);
        } catch (Exception e) {
            // Never let WebSocket failure break the core view-logging flow
            log.warn("[ViewLog] Failed to send owner notification: {}", e.getMessage());
        }
    }

    // ------------------------------------------------------------------
    // Private helpers
    // ------------------------------------------------------------------

    private String extractIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            // X-Forwarded-For can be a comma-separated list; take the first (real client) IP
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private String fingerprintUserAgent(String userAgent) {
        if (userAgent == null || userAgent.isBlank()) return "unknown";
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(userAgent.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(64);
            for (byte b : bytes) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            return "hash-unavailable";
        }
    }

    private ViewLogResponse toMaskedResponse(ViewLog vl) {
        String maskedIp = maskIp(vl.getIpAddress());
        String fpPreview = vl.getDeviceFingerprint() != null && vl.getDeviceFingerprint().length() >= 8
                ? vl.getDeviceFingerprint().substring(0, 8) + "..."
                : vl.getDeviceFingerprint();

        return ViewLogResponse.builder()
                .id(vl.getId())
                .viewerName(vl.getViewer().getName())
                .viewedAt(vl.getViewedAt())
                .maskedIpAddress(maskedIp)
                .consentGiven(vl.getConsentGiven())
                .consentGivenAt(vl.getConsentGivenAt())
                .deviceFingerprintPreview(fpPreview)
                .build();
    }

    /**
     * Masks the last octet of an IPv4 address: "192.168.1.42" → "192.168.1.***"
     * For IPv6 or unusual formats, masks the last segment.
     */
    private String maskIp(String ip) {
        if (ip == null) return "unknown";
        int lastDot = ip.lastIndexOf('.');
        if (lastDot > 0) return ip.substring(0, lastDot) + ".***";
        int lastColon = ip.lastIndexOf(':');
        if (lastColon > 0) return ip.substring(0, lastColon) + ":****";
        return "***";
    }
}
