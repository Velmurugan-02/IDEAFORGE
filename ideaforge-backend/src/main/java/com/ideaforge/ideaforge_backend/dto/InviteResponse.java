package com.ideaforge.ideaforge_backend.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class InviteResponse {
    private Long id;
    private String inviteToken;
    private LocalDateTime expiresAt;
    private Boolean used;
    private LocalDateTime createdAt;
}
