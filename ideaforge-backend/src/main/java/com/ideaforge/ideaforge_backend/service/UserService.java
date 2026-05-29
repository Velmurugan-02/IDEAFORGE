package com.ideaforge.ideaforge_backend.service;

import com.ideaforge.ideaforge_backend.dto.LevelUpEvent;
import com.ideaforge.ideaforge_backend.dto.UserProfileResponse;
import com.ideaforge.ideaforge_backend.model.User;
import com.ideaforge.ideaforge_backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.Optional;

/**
 * User-level operations: profile retrieval and the daily activity streak system.
 *
 * <p>STREAK RULES
 * <ol>
 *   <li>If {@code lastActive} was <b>yesterday</b>: increment {@code streakDays}, update {@code lastActive}.</li>
 *   <li>If {@code lastActive} was <b>today</b>: do nothing (already counted).</li>
 *   <li>If {@code lastActive} was <b>2+ days ago</b> (or null): reset {@code streakDays} to 1.</li>
 *   <li>When {@code streakDays} reaches exactly 7: award {@code SEVEN_DAY_STREAK} XP.</li>
 * </ol>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final XpService xpService;

    // -----------------------------------------------------------------------
    // Profile
    // -----------------------------------------------------------------------

    /** Returns the current user's full profile including XP/level/streak. */
    public UserProfileResponse getProfile(User user) {
        return mapToProfileResponse(user);
    }

    // -----------------------------------------------------------------------
    // Streak
    // -----------------------------------------------------------------------

    /**
     * Should be called after any meaningful user activity (post idea, vote, comment…).
     * Updates the streak and, if the 7-day milestone is hit, awards bonus XP.
     *
     * @return an Optional containing the SEVEN_DAY_STREAK LevelUpEvent if triggered,
     *         or empty otherwise. Callers can merge this into their own LevelUpEvent.
     */
    @Transactional
    public Optional<LevelUpEvent> updateStreak(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        LocalDate today = LocalDate.now();
        LocalDate lastActive = user.getLastActive();

        if (lastActive == null) {
            // First-ever activity
            user.setStreakDays(1);
            user.setLastActive(today);
            userRepository.save(user);
            return Optional.empty();
        }

        if (lastActive.equals(today)) {
            // Already counted today — nothing to do
            return Optional.empty();
        }

        if (lastActive.equals(today.minusDays(1))) {
            // Consecutive day — increment streak
            user.setStreakDays(user.getStreakDays() + 1);
        } else {
            // Missed one or more days — reset streak
            log.info("Streak reset for user {} (id={}): lastActive={}", user.getName(), userId, lastActive);
            user.setStreakDays(1);
        }

        user.setLastActive(today);
        userRepository.save(user);

        // Check 7-day milestone
        if (user.getStreakDays() == 7) {
            log.info("🔥 7-day streak achieved for user {} (id={})", user.getName(), userId);
            LevelUpEvent event = xpService.awardXP(userId, "SEVEN_DAY_STREAK");
            return Optional.of(event);
        }

        return Optional.empty();
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private UserProfileResponse mapToProfileResponse(User user) {
        return UserProfileResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole())
                .xp(user.getXp())
                .level(user.getLevel())
                .streakDays(user.getStreakDays())
                .lastActive(user.getLastActive())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
