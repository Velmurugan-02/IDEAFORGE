package com.ideaforge.ideaforge_backend.dto;

import com.ideaforge.ideaforge_backend.dto.LevelUpEvent;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class IdeaResponse {
    private Long id;
    private String title;
    private String pitch;
    private String problem;
    private String targetAudience;
    private String category;
    private Long userId;
    private String authorName;
    private Double tractionScore;
    private Integer voteCount;
    private Integer commentCount;
    private String battleStatus;
    private String visibility;
    private Boolean isPlagiarised;
    private LocalDateTime createdAt;
    private java.util.List<String> collaborators;
    /** Populated on create — lets the frontend show an XP toast. Null on reads. */
    private LevelUpEvent levelUpEvent;

    /**
     * Set to {@code true} when the authenticated viewer has NOT yet given consent
     * for this idea (Layer-2 consent gate).  Null when the requester is the owner,
     * or when the viewer has already consented.
     */
    private Boolean requiresConsent;
}
