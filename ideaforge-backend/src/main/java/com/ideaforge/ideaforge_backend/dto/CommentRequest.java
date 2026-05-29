package com.ideaforge.ideaforge_backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CommentRequest {
    @NotBlank
    private String content;

    @NotBlank
    private String tag; // SUPPORT, QUESTION, CHALLENGE
}
