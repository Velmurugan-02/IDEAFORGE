package com.ideaforge.ideaforge_backend.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/** Returned by GET /api/admin/ideas/{id}/view-logs — masked for privacy. */
@Data @Builder
public class ViewLogResponse {
    private Long id;
    private String viewerName;
    private LocalDateTime viewedAt;
    /** Only first 3 octets shown (e.g. "192.168.1.***"). */
    private String maskedIpAddress;
    private Boolean consentGiven;
    private LocalDateTime consentGivenAt;
    /** Only first 8 hex characters of the SHA-256 device fingerprint. */
    private String deviceFingerprintPreview;
}
