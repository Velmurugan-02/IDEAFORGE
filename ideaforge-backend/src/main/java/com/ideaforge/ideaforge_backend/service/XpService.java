package com.ideaforge.ideaforge_backend.service;

import com.ideaforge.ideaforge_backend.dto.LevelUpEvent;
import com.ideaforge.ideaforge_backend.model.User;
import com.ideaforge.ideaforge_backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Handles XP awards and level-up detection for all gamification actions.
 *
 * <p>XP VALUES
 * <ul>
 *   <li>IDEA_POSTED        → +50</li>
 *   <li>RECEIVED_UPVOTE    → +10</li>
 *   <li>COMMENT_POSTED     → +5</li>
 *   <li>CHALLENGE_ANSWERED → +100</li>
 *   <li>BATTLE_WON         → +500</li>
 *   <li>BATTLE_VOTE        → +5</li>
 *   <li>SEVEN_DAY_STREAK   → +200</li>
 *   <li>COLLAB_ACCEPTED    → +75</li>
 *   <li>HALL_OF_FAME       → +300</li>
 * </ul>
 *
 * <p>LEVEL THRESHOLDS
 * <ul>
 *   <li>0–199   → Newcomer</li>
 *   <li>200–499 → Thinker</li>
 *   <li>500–999 → Innovator</li>
 *   <li>1000–2499 → Visionary</li>
 *   <li>2500+   → Legend</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class XpService {

    private final UserRepository userRepository;

    // -----------------------------------------------------------------------
    // Public API
    // -----------------------------------------------------------------------

    /**
     * Awards XP to the given user for the specified action, recalculates
     * their level, and returns a {@link LevelUpEvent} that controllers can
     * embed in their API responses.
     *
     * @param userId the target user's ID
     * @param action one of the action constants defined above
     * @return a LevelUpEvent (never null; leveledUp=false when no threshold crossed)
     */
    @Transactional
    public LevelUpEvent awardXP(Long userId, String action) {
        int xpToAdd = xpForAction(action);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        if (xpToAdd == 0) {
            // Unknown action — return a no-op event
            return LevelUpEvent.builder()
                    .leveledUp(false)
                    .newLevel(user.getLevel())
                    .xpGained(0)
                    .totalXP(user.getXp())
                    .build();
        }

        String oldLevel = user.getLevel();
        int newTotalXP = user.getXp() + xpToAdd;

        user.setXp(newTotalXP);
        String newLevel = computeLevel(newTotalXP);
        user.setLevel(newLevel);
        userRepository.save(user);

        boolean leveledUp = !newLevel.equals(oldLevel);
        if (leveledUp) {
            log.info("🎉 User {} (id={}) leveled up: {} → {} (totalXP={})",
                    user.getName(), userId, oldLevel, newLevel, newTotalXP);
        }

        return LevelUpEvent.builder()
                .leveledUp(leveledUp)
                .newLevel(newLevel)
                .xpGained(xpToAdd)
                .totalXP(newTotalXP)
                .build();
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    /** Maps an action string to its XP reward. Returns 0 for unknown actions. */
    private int xpForAction(String action) {
        if (action == null) return 0;
        return switch (action) {
            case "IDEA_POSTED"        -> 50;
            case "RECEIVED_UPVOTE"    -> 10;
            case "COMMENT_POSTED"     -> 5;
            case "CHALLENGE_ANSWERED" -> 100;
            case "BATTLE_WON"         -> 500;
            case "BATTLE_VOTE"        -> 5;
            case "SEVEN_DAY_STREAK"   -> 200;
            case "COLLAB_ACCEPTED"    -> 75;
            case "HALL_OF_FAME"       -> 300;
            default -> {
                log.warn("Unknown XP action: '{}'", action);
                yield 0;
            }
        };
    }

    /** Computes the level string from cumulative XP. */
    public static String computeLevel(int totalXP) {
        if (totalXP >= 2500) return "Legend";
        if (totalXP >= 1000) return "Visionary";
        if (totalXP >= 500)  return "Innovator";
        if (totalXP >= 200)  return "Thinker";
        return "Newcomer";
    }
}
