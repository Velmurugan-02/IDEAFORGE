package com.ideaforge.ideaforge_backend.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * A digital certificate issued to an idea that has been validated and protected
 * on the IdeaForge platform.
 *
 * Each idea can have at most one certificate (unique constraint on idea_id).
 * The certificate_code is a short, human-readable unique identifier displayed
 * on the certificate document.
 */
@Entity
@Table(name = "idea_certificates")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IdeaCertificate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The protected idea. One-to-one relationship enforced via @Column(unique = true).
     * Using ManyToOne here (instead of OneToOne) provides more flexibility for
     * queries while the unique constraint ensures the 1:1 relationship at DB level.
     */
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "idea_id", nullable = false, unique = true)
    private Idea idea;

    /** Short printable code, e.g. "IF-2024-A7X9K". Unique across all certificates. */
    @Column(name = "certificate_code", nullable = false, unique = true, length = 100)
    private String certificateCode;

    @Column(name = "issued_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime issuedAt = LocalDateTime.now();
}
