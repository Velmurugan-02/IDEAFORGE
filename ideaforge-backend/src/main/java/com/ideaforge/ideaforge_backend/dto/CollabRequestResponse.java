package com.ideaforge.ideaforge_backend.dto;

import com.ideaforge.ideaforge_backend.dto.LevelUpEvent;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class CollabRequestResponse {
    private Long id;
    private Long ideaId;
    private String ideaTitle;
    private Long requesterId;
    private String requesterName;
    private Long ownerId;
    private String ownerName;
    private String message;
    private String status;
    private LocalDateTime createdAt;
    /** XP event from COLLAB_ACCEPTED — populated only on accept. Null on list reads. */
    private LevelUpEvent levelUpEvent;
}
