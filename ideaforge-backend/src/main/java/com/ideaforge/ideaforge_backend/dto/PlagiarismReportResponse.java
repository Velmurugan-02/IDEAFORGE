package com.ideaforge.ideaforge_backend.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class PlagiarismReportResponse {
    private Long id;
    private Long reportedIdeaId;
    private Long originalIdeaId;
    private Long reportedById;
    /** Display name of the user who filed the report. */
    private String reporterName;
    private String reason;
    private String status;
    private LocalDateTime createdAt;
}
