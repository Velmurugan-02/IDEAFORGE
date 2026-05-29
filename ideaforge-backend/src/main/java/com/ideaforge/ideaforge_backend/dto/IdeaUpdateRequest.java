package com.ideaforge.ideaforge_backend.dto;

import lombok.Data;

@Data
public class IdeaUpdateRequest {
    private String title;
    private String pitch;
    private String problem;
    private String targetAudience;
    private String category;
    private String visibility;
}
