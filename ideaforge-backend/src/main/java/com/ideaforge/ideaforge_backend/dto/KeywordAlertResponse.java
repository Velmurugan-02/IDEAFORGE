package com.ideaforge.ideaforge_backend.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/** Returned by GET /api/users/me/keyword-alerts */
@Data @Builder
public class KeywordAlertResponse {
    private Long id;
    private Long watchId;
    private String matchedKeyword;
    private String sourceUrl;
    private String sourceTitle;
    private LocalDateTime detectedAt;
    private Boolean isRead;
    /** The idea title for context. */
    private String ideaTitle;
}
