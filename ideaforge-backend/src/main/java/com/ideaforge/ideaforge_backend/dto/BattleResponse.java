package com.ideaforge.ideaforge_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BattleResponse {
    private Long id;
    private IdeaResponse ideaA;
    private IdeaResponse ideaB;
    private int votesA;
    private int votesB;
    private double percentA;
    private double percentB;
    private long timeRemainingSeconds;
    private String status;
    private Long winnerId;
}
