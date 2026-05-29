package com.ideaforge.ideaforge_backend.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * Spring Security configuration for the IdeaForge API.
 *
 * Security model:
 *   - Stateless JWT - no HTTP sessions created or used.
 *   - CSRF disabled (safe for stateless REST APIs consumed by SPA clients).
 *   - CORS configured to allow the React dev server origins.
 *
 * Public endpoints:
 *   - POST /api/auth/**           - register and login
 *   - GET  /api/hall-of-fame      - weekly leaderboard (read-only, unauthenticated)
 *   - GET  /api/ideas/x/certificate - public certificate verification
 *   - POST /api/admin/setup       - initial admin setup
 *
 * Everything else requires a valid JWT Bearer token.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter          jwtAuthFilter;
    private final UserDetailsServiceImpl userDetailsService;

    /** Comma-separated origins; supports Vercel / Render frontends when set in env. */
    @org.springframework.beans.factory.annotation.Value(
            "${ideaforge.cors.allowed-origins:http://localhost:3000,http://localhost:5173}")
    private String corsAllowedOrigins;

    // ------------------------------------------------------------------
    // Security filter chain
    // ------------------------------------------------------------------

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // Disable CSRF - not needed for stateless JWT APIs
            .csrf(AbstractHttpConfigurer::disable)

            // CORS - allow React dev origins
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))

            // Authorization rules
            .authorizeHttpRequests(auth -> auth

                // Public: auth endpoints
                .requestMatchers("/api/auth/**").permitAll()
                
                // Public: admin setup
                .requestMatchers(HttpMethod.POST, "/api/admin/setup").permitAll()

                // Public: read-only Hall of Fame
                .requestMatchers(HttpMethod.GET, "/api/hall-of-fame").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/hall-of-fame/**").permitAll()

                // Public: certificate verification
                .requestMatchers(HttpMethod.GET, "/api/ideas/*/certificate").permitAll()

                // Public: viewing ideas
                .requestMatchers(HttpMethod.GET, "/api/ideas").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/ideas/**").permitAll()

                // Public: user badge profiles
                .requestMatchers(HttpMethod.GET, "/api/users/*/badges").permitAll()

                // Everything else requires authentication
                .anyRequest().authenticated()
            )

            // Stateless session - never create or use HTTP session
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )

            // Register our custom AuthenticationProvider
            .authenticationProvider(authenticationProvider())

            // JWT filter runs before Spring's username/password filter
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    // ------------------------------------------------------------------
    // Authentication provider
    // ------------------------------------------------------------------

    /**
     * DAO-based provider that uses UserDetailsServiceImpl and BCrypt
     * to authenticate credentials supplied at login.
     */
    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    /**
     * Expose the AuthenticationManager so AuthController
     * can trigger authentication programmatically.
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config)
            throws Exception {
        return config.getAuthenticationManager();
    }

    // ------------------------------------------------------------------
    // Password encoder
    // ------------------------------------------------------------------

    /** BCrypt with default strength (10 rounds) - industry-standard for password hashing. */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // ------------------------------------------------------------------
    // CORS
    // ------------------------------------------------------------------

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        java.util.LinkedHashSet<String> patterns = new java.util.LinkedHashSet<>();
        patterns.add("http://localhost:3000");
        patterns.add("http://localhost:5173");
        patterns.add("https://*.vercel.app");
        patterns.add("https://*.onrender.com");
        Arrays.stream(corsAllowedOrigins.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .forEach(patterns::add);

        config.setAllowedOriginPatterns(List.copyOf(patterns));

        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L); // preflight cache for 1 hour

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
