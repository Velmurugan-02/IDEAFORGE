package com.ideaforge.ideaforge_backend.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Represents a single earned badge returned by GET /api/users/{id}/badges.
 */
@Data
@Builder
public class BadgeResponse {
    private Long id;
    private String type;
    private LocalDateTime earnedAt;
}
