package com.ideaforge.ideaforge_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminPlatformStatsResponse {
    private long totalUsers;
    private long totalIdeas;
    private long totalVotes;
    /** Completed battles (fought to resolution). */
    private long totalBattlesFought;
}
