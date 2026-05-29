package com.ideaforge.ideaforge_backend.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class IdeaCreateRequest {
    @NotBlank(message = "Title is required")
    private String title;

    @NotBlank(message = "Pitch is required")
    private String pitch;

    @NotBlank(message = "Problem is required")
    private String problem;

    private String targetAudience;
    private String category;
    private String visibility;

    @NotNull(message = "agreedToTerms is required")
    @AssertTrue(message = "You must agree to the terms before posting")
    private Boolean agreedToTerms;
}
