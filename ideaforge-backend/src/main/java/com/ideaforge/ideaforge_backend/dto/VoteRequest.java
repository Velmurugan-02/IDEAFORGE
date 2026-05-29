package com.ideaforge.ideaforge_backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class VoteRequest {
    @NotBlank
    private String type; // UP or DOWN
}
