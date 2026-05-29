package com.ideaforge.ideaforge_backend.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/** Returned by GET /api/ideas/{id}/hash */
@Data @Builder
public class IdeaHashResponse {
    private Long ideaId;
    private String sha256Hash;
    private LocalDateTime generatedAt;
    private String verificationNote;
}
