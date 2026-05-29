package com.ideaforge.ideaforge_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Returned by XpService.awardXP() and included in every API response that
 * triggers an XP gain. The frontend reads this object to show a toast
 * notification (and a level-up animation when leveledUp = true).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LevelUpEvent {

    /** Whether the user crossed a level threshold with this XP award. */
    private boolean leveledUp;

    /** The user's new level string (always present, even when no level-up). */
    private String newLevel;

    /** XP awarded for the triggering action. */
    private int xpGained;

    /** User's cumulative XP after this award. */
    private int totalXP;
}
