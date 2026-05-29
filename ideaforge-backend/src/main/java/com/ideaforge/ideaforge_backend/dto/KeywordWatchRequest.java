package com.ideaforge.ideaforge_backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** Request body for POST /api/ideas/{id}/keywords */
@Data
public class KeywordWatchRequest {

    @NotBlank(message = "Keyword must not be blank")
    @Size(min = 2, max = 100, message = "Keyword must be between 2 and 100 characters")
    private String keyword;
}
