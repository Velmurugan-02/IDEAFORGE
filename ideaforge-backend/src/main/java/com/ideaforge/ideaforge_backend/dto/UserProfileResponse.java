package com.ideaforge.ideaforge_backend.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Returned by GET /api/users/me — full user profile including XP gamification fields.
 */
@Data
@Builder
public class UserProfileResponse {
    private Long id;
    private String name;
    private String email;
    private String role;
    private Integer xp;
    private String level;
    private Integer streakDays;
    private LocalDate lastActive;
    private LocalDateTime createdAt;
}
