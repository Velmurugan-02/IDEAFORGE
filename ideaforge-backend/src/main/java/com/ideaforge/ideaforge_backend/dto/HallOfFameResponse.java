package com.ideaforge.ideaforge_backend.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Represents one Hall of Fame entry returned by GET /api/hall-of-fame.
 * Grouped by weekStart on the frontend.
 */
@Data
@Builder
public class HallOfFameResponse {
    private Long   id;
    private LocalDate weekStart;
    private Integer   rank;
    private BigDecimal snapshotScore;

    /** Idea details embedded in the entry. */
    private Long   ideaId;
    private String ideaTitle;
    private String ideaPitch;
    private String category;

    /** Owner info. */
    private Long   ownerId;
    private String ownerName;
}
