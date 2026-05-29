package com.ideaforge.ideaforge_backend.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Stores the immutable SHA-256 hash of an idea at the moment it was posted.
 * This hash serves as proof that the idea content has not been altered since submission.
 * One hash per idea (UNIQUE constraint on idea_id).
 */
@Entity
@Table(name = "idea_hashes")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class IdeaHash {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** One-to-one relationship with the idea. */
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "idea_id", nullable = false, unique = true)
    private Idea idea;

    /** Hexadecimal SHA-256 hash (64 characters). */
    @Column(name = "sha256_hash", nullable = false, length = 64)
    private String sha256Hash;

    /**
     * The exact string that was hashed: title + "|" + pitch + "|" + userId + "|" + createdAt.
     * Stored for independent verification.
     */
    @Column(name = "hash_input", nullable = false, columnDefinition = "TEXT")
    private String hashInput;

    /** Timestamp at which the hash was generated (= idea creation time). */
    @Column(name = "generated_at", nullable = false)
    @Builder.Default
    private LocalDateTime generatedAt = LocalDateTime.now();
}
