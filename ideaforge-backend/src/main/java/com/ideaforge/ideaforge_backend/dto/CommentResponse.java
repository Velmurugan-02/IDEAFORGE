package com.ideaforge.ideaforge_backend.dto;

import com.ideaforge.ideaforge_backend.dto.LevelUpEvent;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class CommentResponse {
    private Long id;
    private Long ideaId;
    private Long userId;
    private String userName;
    private String content;
    private String tag;
    private Boolean answered;
    private LocalDateTime createdAt;
    /** XP event from COMMENT_POSTED or CHALLENGE_ANSWERED. Null on list reads. */
    private LevelUpEvent levelUpEvent;
}
