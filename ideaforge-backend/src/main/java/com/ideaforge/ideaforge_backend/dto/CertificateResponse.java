package com.ideaforge.ideaforge_backend.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Full certificate data returned to the frontend certificate page.
 * postedAt is the idea's original createdAt (= timestamp of first submission).
 */
@Data
@Builder
public class CertificateResponse {
    private String        certificateCode;
    private String        ideaTitle;
    private String        ideaPitch;
    private String        ownerName;
    /** Timestamp of the first submission (idea.createdAt). */
    private LocalDateTime issuedAt;
    private Long          ideaId;
}
