package com.ideaforge.ideaforge_backend.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PlagiarismReportRequest {
    @NotNull(message = "Original idea ID is required")
    private Long originalIdeaId;
    
    private String reason;
}
