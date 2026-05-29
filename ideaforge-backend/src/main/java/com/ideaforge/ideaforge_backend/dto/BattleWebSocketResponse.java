package com.ideaforge.ideaforge_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BattleWebSocketResponse {
    private int votesA;
    private int votesB;
    private double percentA;
    private double percentB;
    private int totalVotes;
    private boolean completed;
    private Long winnerId;
    private String winnerTitle;
}
