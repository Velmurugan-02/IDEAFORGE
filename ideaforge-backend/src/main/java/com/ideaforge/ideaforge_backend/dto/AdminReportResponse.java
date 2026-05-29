package com.ideaforge.ideaforge_backend.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AdminReportResponse {
    private PlagiarismReportResponse report;
    private IdeaResponse reportedIdea;
    private IdeaResponse originalIdea;
}
