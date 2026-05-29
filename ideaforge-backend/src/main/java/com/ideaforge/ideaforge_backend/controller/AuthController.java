package com.ideaforge.ideaforge_backend.controller;

import com.ideaforge.ideaforge_backend.dto.AuthResponse;
import com.ideaforge.ideaforge_backend.dto.LoginRequest;
import com.ideaforge.ideaforge_backend.dto.RegisterRequest;
import com.ideaforge.ideaforge_backend.model.User;
import com.ideaforge.ideaforge_backend.repository.UserRepository;
import com.ideaforge.ideaforge_backend.security.JwtUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * REST controller for authentication operations.
 *
 * <p>All endpoints under {@code /api/auth} are publicly accessible (configured in
 * {@link com.ideaforge.ideaforge_backend.security.SecurityConfig}).
 */
@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserRepository       userRepository;
    private final PasswordEncoder      passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil              jwtUtil;

    // ──────────────────────────────────────────────────────────────────
    // POST /api/auth/register
    // ──────────────────────────────────────────────────────────────────

    /**
     * Register a new user account.
     *
     * <p>Steps:
     * <ol>
     *   <li>Validate request body (Jakarta Validation).</li>
     *   <li>Check for duplicate email — return 400 if taken.</li>
     *   <li>Hash password with BCrypt and persist the user.</li>
     *   <li>Generate JWT and return {@link AuthResponse}.</li>
     * </ol>
     *
     * @param request validated registration payload
     * @return 201 Created with {@link AuthResponse}, or 400 with an error map
     */
    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {

        // Guard: duplicate email
        if (userRepository.existsByEmail(request.getEmail())) {
            log.warn("Registration attempt with already-registered email: {}", request.getEmail());
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Email already registered"));
        }

        // Build and persist new user
        User newUser = User.builder()
                .name(request.getName())
                .email(request.getEmail().toLowerCase().trim())
                .password(passwordEncoder.encode(request.getPassword()))
                .xp(0)
                .level("Newcomer")
                .streakDays(0)
                .lastActive(LocalDate.now())
                .createdAt(LocalDateTime.now())
                .build();

        User saved = userRepository.save(newUser);
        log.info("New user registered: id={}, email={}", saved.getId(), saved.getEmail());

        // Issue JWT
        String token = jwtUtil.generateToken(saved.getEmail());

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(buildAuthResponse(token, saved));
    }

    // ──────────────────────────────────────────────────────────────────
    // POST /api/auth/login
    // ──────────────────────────────────────────────────────────────────

    /**
     * Authenticate an existing user and issue a JWT.
     *
     * <p>Delegates credential verification to Spring Security's
     * {@link AuthenticationManager}, which invokes
     * {@link com.ideaforge.ideaforge_backend.security.UserDetailsServiceImpl}
     * and BCrypt comparison internally.
     *
     * @param request validated login payload
     * @return 200 OK with {@link AuthResponse}, or 401 with an error map
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {

        try {
            // Throws BadCredentialsException if credentials are wrong
            Authentication auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getEmail().toLowerCase().trim(),
                            request.getPassword()
                    )
            );

            // Spring Security resolved the principal — cast it back to our User entity
            User user = (User) auth.getPrincipal();

            // Update last_active and streak on every login
            updateLoginActivity(user);

            String token = jwtUtil.generateToken(user.getEmail());
            log.info("User logged in: id={}, email={}", user.getId(), user.getEmail());

            return ResponseEntity.ok(buildAuthResponse(token, user));

        } catch (BadCredentialsException e) {
            log.warn("Failed login attempt for email: {}", request.getEmail());
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid credentials"));
        }
    }

    // ──────────────────────────────────────────────────────────────────
    // Private helpers
    // ──────────────────────────────────────────────────────────────────

    /** Map a {@link User} + token to the API response DTO. */
    private AuthResponse buildAuthResponse(String token, User user) {
        return AuthResponse.builder()
                .token(token)
                .userId(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .level(user.getLevel())
                .xp(user.getXp())
                .build();
    }

    /**
     * Update {@code last_active} and {@code streak_days} on each successful login.
     * A streak is maintained if the user logged in yesterday; resets to 1 otherwise.
     */
    private void updateLoginActivity(User user) {
        LocalDate today     = LocalDate.now();
        LocalDate lastActive = user.getLastActive();

        if (lastActive == null || lastActive.isBefore(today)) {
            // Increment streak if consecutive day; otherwise reset
            if (lastActive != null && lastActive.equals(today.minusDays(1))) {
                user.setStreakDays(user.getStreakDays() + 1);
            } else if (lastActive == null || lastActive.isBefore(today.minusDays(1))) {
                user.setStreakDays(1);
            }
            user.setLastActive(today);
            userRepository.save(user);
        }
    }
}
