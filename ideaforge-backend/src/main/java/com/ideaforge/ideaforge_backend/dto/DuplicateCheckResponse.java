package com.ideaforge.ideaforge_backend.dto;

import lombok.Builder;
import lombok.Data;

/**
 * Response from POST /api/ideas/check-duplicate.
 *
 * Fields:
 *   similarityFound   — true if at least one similar idea was found.
 *   similarityScore   — 0–100 percentage similarity with the closest match.
 *   similarIdeaId     — ID of the most-similar idea (null if none).
 *   similarIdeaTitle  — Title of the most-similar idea (null if none).
 */
@Data
@Builder
public class DuplicateCheckResponse {
    private boolean similarityFound;
    private int     similarityScore;
    private Long    similarIdeaId;
    private String  similarIdeaTitle;
}
