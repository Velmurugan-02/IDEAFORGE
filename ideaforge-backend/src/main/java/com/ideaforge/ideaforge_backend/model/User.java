package com.ideaforge.ideaforge_backend.model;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

/**
 * Represents a registered user on the IdeaForge platform.
 * Implements UserDetails so it can be used directly with Spring Security.
 */
@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @Column(nullable = false)
    private String password;

    /** Experience points – incremented when user submits ideas, earns votes, etc. */
    @Column(nullable = false)
    @Builder.Default
    private Integer xp = 0;

    /** Human-readable rank derived from XP (e.g. Newcomer → Innovator → Visionary). */
    @Column(nullable = false, length = 50)
    @Builder.Default
    private String level = "Newcomer";

    /** Consecutive-day activity streak. */
    @Column(name = "streak_days", nullable = false)
    @Builder.Default
    private Integer streakDays = 0;

    /** Date of last platform activity – used to compute streaks. */
    @Column(name = "last_active")
    private LocalDate lastActive;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(nullable = false, length = 50)
    @Builder.Default
    private String role = "ROLE_USER";

    // ------------------------------------------------------------------
    // UserDetails – Spring Security integration
    // ------------------------------------------------------------------

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority(role));
    }

    /** Email is used as the unique username for authentication. */
    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override public boolean isAccountNonExpired()  { return true; }
    @Override public boolean isAccountNonLocked()   { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled()             { return true; }
}
