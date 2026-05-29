package com.ideaforge.ideaforge_backend.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/** Returned by GET /api/ideas/{id}/keywords */
@Data @Builder
public class KeywordWatchResponse {
    private Long id;
    private String keyword;
    private Long ideaId;
    private LocalDateTime createdAt;
}
