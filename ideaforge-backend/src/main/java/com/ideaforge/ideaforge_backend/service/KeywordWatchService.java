package com.ideaforge.ideaforge_backend.service;

import com.ideaforge.ideaforge_backend.dto.KeywordAlertResponse;
import com.ideaforge.ideaforge_backend.dto.KeywordWatchRequest;
import com.ideaforge.ideaforge_backend.dto.KeywordWatchResponse;
import com.ideaforge.ideaforge_backend.model.Idea;
import com.ideaforge.ideaforge_backend.model.KeywordAlert;
import com.ideaforge.ideaforge_backend.model.KeywordWatch;
import com.ideaforge.ideaforge_backend.model.User;
import com.ideaforge.ideaforge_backend.repository.IdeaRepository;
import com.ideaforge.ideaforge_backend.repository.KeywordAlertRepository;
import com.ideaforge.ideaforge_backend.repository.KeywordWatchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Layer 5 — Keyword Watch
 *
 * Allows idea owners to register up to 5 keywords per idea.
 * A daily scheduled job scans Google News RSS for each keyword and stores
 * any new matches as {@link KeywordAlert} entries, then sends a WebSocket
 * notification to the idea owner.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KeywordWatchService {

    private static final int MAX_KEYWORDS_PER_IDEA = 5;
    private static final String GOOGLE_NEWS_RSS =
            "https://news.google.com/rss/search?q=%s&hl=en-US&gl=US&ceid=US:en";

    private final KeywordWatchRepository  watchRepository;
    private final KeywordAlertRepository  alertRepository;
    private final IdeaRepository          ideaRepository;
    private final SimpMessagingTemplate   messagingTemplate;

    // ------------------------------------------------------------------
    // Keyword Management
    // ------------------------------------------------------------------

    /**
     * POST /api/ideas/{id}/keywords
     * Registers a new keyword for monitoring. Max 5 per idea.
     */
    @Transactional
    public KeywordWatchResponse addKeyword(Long ideaId, KeywordWatchRequest request, User owner) {
        Idea idea = requireOwner(ideaId, owner);

        long existing = watchRepository.countByIdeaId(ideaId);
        if (existing >= MAX_KEYWORDS_PER_IDEA) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Maximum " + MAX_KEYWORDS_PER_IDEA + " keywords allowed per idea");
        }

        KeywordWatch watch = KeywordWatch.builder()
                .idea(idea)
                .user(owner)
                .keyword(request.getKeyword().trim().toLowerCase())
                .build();

        watch = watchRepository.save(watch);
        return toWatchResponse(watch);
    }

    /**
     * GET /api/ideas/{id}/keywords
     * Lists all keyword watches for an idea (owner only).
     */
    @Transactional(readOnly = true)
    public List<KeywordWatchResponse> listKeywords(Long ideaId, User owner) {
        requireOwner(ideaId, owner);
        return watchRepository.findByIdeaId(ideaId).stream()
                .map(this::toWatchResponse)
                .collect(Collectors.toList());
    }

    /**
     * DELETE /api/ideas/{id}/keywords/{keywordId}
     * Removes a keyword watch (owner only).
     */
    @Transactional
    public void removeKeyword(Long ideaId, Long keywordId, User owner) {
        requireOwner(ideaId, owner);
        KeywordWatch watch = watchRepository.findById(keywordId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Keyword watch not found"));
        if (!watch.getIdea().getId().equals(ideaId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Keyword does not belong to this idea");
        }
        watchRepository.delete(watch);
    }

    // ------------------------------------------------------------------
    // Keyword Alerts (for the owner's inbox)
    // ------------------------------------------------------------------

    /**
     * GET /api/users/me/keyword-alerts
     * Returns all keyword alerts for the authenticated user.
     */
    @Transactional
    public List<KeywordAlertResponse> getMyAlerts(User user) {
        List<KeywordAlert> alerts =
                alertRepository.findByUserIdOrderByDetectedAtDesc(user.getId());
        // Mark all as read
        alerts.stream().filter(a -> !a.getIsRead()).forEach(a -> {
            a.setIsRead(true);
            alertRepository.save(a);
        });
        return alerts.stream().map(this::toAlertResponse).collect(Collectors.toList());
    }

    // ------------------------------------------------------------------
    // Layer 5 — Scheduled Keyword Scanner (runs once every 24 hours)
    // ------------------------------------------------------------------

    /**
     * Scans Google News RSS for every registered keyword.
     * New matches are saved as {@link KeywordAlert} entries and the owner is
     * notified via WebSocket.
     *
     * Fixed-rate: 86 400 000 ms = 24 hours.
     * Initial delay: 60 000 ms = 1 minute after startup (avoids startup spike).
     */
    @Scheduled(fixedRate = 86_400_000, initialDelay = 60_000)
    public void scanKeywords() {
        List<KeywordWatch> watches = watchRepository.findAll();
        if (watches.isEmpty()) return;

        log.info("[KeywordScan] Starting scan for {} keyword watches", watches.size());
        int newAlerts = 0;

        for (KeywordWatch watch : watches) {
            try {
                newAlerts += scanSingleKeyword(watch);
            } catch (Exception e) {
                log.warn("[KeywordScan] Failed to scan keyword '{}': {}", watch.getKeyword(), e.getMessage());
            }
        }

        log.info("[KeywordScan] Scan complete. {} new alerts generated.", newAlerts);
    }

    // ------------------------------------------------------------------
    // Private helpers
    // ------------------------------------------------------------------

    /**
     * Fetches the Google News RSS feed for a keyword, parses each {@code <item>},
     * and saves new alerts (deduped by source URL).
     *
     * @return number of new alerts created
     */
    private int scanSingleKeyword(KeywordWatch watch) throws Exception {
        String encodedKeyword = URLEncoder.encode(watch.getKeyword(), StandardCharsets.UTF_8);
        String rssUrl = String.format(GOOGLE_NEWS_RSS, encodedKeyword);

        HttpURLConnection conn = (HttpURLConnection) new URL(rssUrl).openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(8_000);
        conn.setReadTimeout(10_000);
        // Mimic a browser request to avoid 429 responses
        conn.setRequestProperty("User-Agent",
                "Mozilla/5.0 (compatible; IdeaForgeBot/1.0; +https://ideaforge.app/bot)");

        int responseCode = conn.getResponseCode();
        if (responseCode != 200) {
            log.debug("[KeywordScan] RSS returned {} for keyword '{}'", responseCode, watch.getKeyword());
            return 0;
        }

        int created = 0;
        try (InputStream is = conn.getInputStream()) {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            // Disable external entity processing (XXE prevention)
            dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            Document doc = dbf.newDocumentBuilder().parse(is);
            doc.getDocumentElement().normalize();

            NodeList items = doc.getElementsByTagName("item");
            for (int i = 0; i < items.getLength() && i < 10; i++) {
                org.w3c.dom.Element item = (org.w3c.dom.Element) items.item(i);

                String title = getTagValue("title", item);
                String link  = getTagValue("link",  item);

                if (title == null || link == null || link.isBlank()) continue;

                // Dedup — skip if we already have an alert for this watch + URL
                if (alertRepository.existsByWatchIdAndSourceUrl(watch.getId(), link)) continue;

                KeywordAlert alert = KeywordAlert.builder()
                        .watch(watch)
                        .user(watch.getUser())
                        .matchedKeyword(watch.getKeyword())
                        .sourceUrl(link)
                        .sourceTitle(title)
                        .build();
                alertRepository.save(alert);
                created++;

                // WebSocket notification to the owner
                notifyOwner(watch, title, link);
            }
        }

        return created;
    }

    private void notifyOwner(KeywordWatch watch, String articleTitle, String articleUrl) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("type",    "KEYWORD_ALERT");
            payload.put("message", "Keyword '" + watch.getKeyword()
                    + "' was found in: " + articleTitle);
            payload.put("keyword",    watch.getKeyword());
            payload.put("articleUrl", articleUrl);
            payload.put("ideaId",     watch.getIdea().getId());

            messagingTemplate.convertAndSend(
                    "/topic/user/" + watch.getUser().getId() + "/notifications", payload);
        } catch (Exception e) {
            log.warn("[KeywordScan] WebSocket notification failed: {}", e.getMessage());
        }
    }

    /** Safely extracts the text content of the first matching child element. */
    private String getTagValue(String tag, org.w3c.dom.Element parent) {
        NodeList nodes = parent.getElementsByTagName(tag);
        if (nodes == null || nodes.getLength() == 0) return null;
        return nodes.item(0).getTextContent();
    }

    /** Verifies the requester is the idea owner and returns the idea entity. */
    private Idea requireOwner(Long ideaId, User owner) {
        Idea idea = ideaRepository.findById(ideaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Idea not found"));
        if (!idea.getUser().getId().equals(owner.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Only the idea owner can manage keyword watches");
        }
        return idea;
    }

    private KeywordWatchResponse toWatchResponse(KeywordWatch w) {
        return KeywordWatchResponse.builder()
                .id(w.getId())
                .keyword(w.getKeyword())
                .ideaId(w.getIdea().getId())
                .createdAt(w.getCreatedAt())
                .build();
    }

    private KeywordAlertResponse toAlertResponse(KeywordAlert a) {
        return KeywordAlertResponse.builder()
                .id(a.getId())
                .watchId(a.getWatch().getId())
                .matchedKeyword(a.getMatchedKeyword())
                .sourceUrl(a.getSourceUrl())
                .sourceTitle(a.getSourceTitle())
                .detectedAt(a.getDetectedAt())
                .isRead(a.getIsRead())
                .ideaTitle(a.getWatch().getIdea().getTitle())
                .build();
    }
}
