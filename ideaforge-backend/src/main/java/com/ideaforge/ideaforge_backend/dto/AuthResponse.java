package com.ideaforge.ideaforge_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response body returned after successful register or login.
 * Contains the JWT bearer token plus enough user info for the React
 * frontend to bootstrap its auth state without an extra /me call.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {

    /** JWT bearer token — include as Authorization: Bearer <token> on subsequent requests. */
    private String token;

    private Long   userId;
    private String name;
    private String email;

    /** Current gamification level (e.g. "Newcomer", "Innovator"). */
    private String level;

    /** Accumulated experience points. */
    private Integer xp;
}
