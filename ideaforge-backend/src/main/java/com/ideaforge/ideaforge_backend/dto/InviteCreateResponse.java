package com.ideaforge.ideaforge_backend.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class InviteCreateResponse {
    private String inviteToken;
    private String shareableLink;
    private String expiresAt;
}
