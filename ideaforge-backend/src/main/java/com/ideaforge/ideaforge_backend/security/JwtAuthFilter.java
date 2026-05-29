package com.ideaforge.ideaforge_backend.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Servlet filter that runs once per request and wires JWT-based authentication
 * into Spring Security's SecurityContextHolder.
 *
 * Flow:
 *   1. Read "Authorization: Bearer token" header.
 *   2. Validate the token with JwtUtil.
 *   3. Load the UserDetails from the database.
 *   4. Set a fully-authenticated UsernamePasswordAuthenticationToken
 *      in the SecurityContextHolder.
 *
 * If any step fails the filter passes the request along unauthenticated
 * and Spring Security's access control rules will reject it if the endpoint
 * requires auth.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtil                jwtUtil;
    private final UserDetailsServiceImpl userDetailsService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest  request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain         filterChain
    ) throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");

        // Skip filter if no Bearer token is present
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        final String token = authHeader.substring(7); // strip "Bearer "

        // Skip if token is malformed or expired
        if (!jwtUtil.validateToken(token)) {
            log.debug("JWT validation failed for request: {}", request.getRequestURI());
            filterChain.doFilter(request, response);
            return;
        }

        final String email = jwtUtil.extractEmail(token);

        // Only set authentication if the context is currently empty (not already authenticated)
        if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                UserDetails userDetails = userDetailsService.loadUserByUsername(email);

                UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(
                                userDetails,
                                null,                        // credentials not needed post-auth
                                userDetails.getAuthorities()
                        );
                authToken.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request)
                );

                SecurityContextHolder.getContext().setAuthentication(authToken);
                log.debug("Authenticated user '{}' via JWT", email);

            } catch (Exception e) {
                log.warn("Could not load user '{}' from token: {}", email, e.getMessage());
            }
        }

        filterChain.doFilter(request, response);
    }
}
