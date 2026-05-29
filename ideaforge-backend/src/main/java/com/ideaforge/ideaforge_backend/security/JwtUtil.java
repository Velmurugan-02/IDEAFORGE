package com.ideaforge.ideaforge_backend.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * Stateless JWT utility - token generation, validation, and claim extraction.
 *
 * Uses JJWT 0.12.x API (Keys.hmacShaKeyFor / Jwts.builder /
 * Jwts.parser().verifyWith().build()).
 */
@Component
public class JwtUtil {

    private final SecretKey secretKey;
    private final long expirationMs;

    public JwtUtil(
            @Value("${ideaforge.jwt.secret}") String secret,
            @Value("${ideaforge.jwt.expiration-ms}") long expirationMs) {
        this.secretKey    = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    // ------------------------------------------------------------------
    // Public API
    // ------------------------------------------------------------------

    /**
     * Generate a signed JWT token whose subject is the user's email.
     *
     * @param email the authenticated user's email address
     * @return compact JWT string
     */
    public String generateToken(String email) {
        Date now    = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .subject(email)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(secretKey)
                .compact();
    }

    /**
     * Validate that a token is well-formed, correctly signed, and not expired.
     *
     * @param token compact JWT string
     * @return true if the token is valid
     */
    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Extract the email (subject) from a token.
     * Only call this after validateToken returns true.
     *
     * @param token compact JWT string
     * @return the email embedded as the subject claim
     */
    public String extractEmail(String token) {
        return parseClaims(token).getSubject();
    }

    // ------------------------------------------------------------------
    // Private helpers
    // ------------------------------------------------------------------

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
