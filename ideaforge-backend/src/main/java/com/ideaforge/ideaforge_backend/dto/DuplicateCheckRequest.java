package com.ideaforge.ideaforge_backend.dto;

import lombok.Data;

/**
 * Request body for the duplicate-check endpoint.
 * Called before the user finalises their idea submission.
 */
@Data
public class DuplicateCheckRequest {
    private String title;
    private String pitch;
}
