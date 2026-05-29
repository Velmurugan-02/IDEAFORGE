-- =====================================================
-- IdeaForge Database Schema
-- Database: ideaforge_db
-- Engine: MySQL 8.x
-- =====================================================

CREATE DATABASE IF NOT EXISTS ideaforge_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE ideaforge_db;

-- -------------------------------------------------------
-- 1. users
-- -------------------------------------------------------
CREATE TABLE IF NOT EXISTS users (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    name        VARCHAR(100) NOT NULL,
    email       VARCHAR(150) NOT NULL UNIQUE,
    password    VARCHAR(255) NOT NULL,
    xp          INT          NOT NULL DEFAULT 0,
    level       VARCHAR(50)  NOT NULL DEFAULT 'Newcomer',
    streak_days INT          NOT NULL DEFAULT 0,
    last_active DATE,
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- -------------------------------------------------------
-- 2. ideas
-- -------------------------------------------------------
CREATE TABLE IF NOT EXISTS ideas (
    id               BIGINT          NOT NULL AUTO_INCREMENT,
    title            VARCHAR(200)    NOT NULL,
    pitch            TEXT,
    problem          TEXT,
    target_audience  VARCHAR(100),
    category         VARCHAR(50),
    user_id          BIGINT          NOT NULL,
    traction_score   DECIMAL(10,2)   NOT NULL DEFAULT 0.00,
    vote_count       INT             NOT NULL DEFAULT 0,
    comment_count    INT             NOT NULL DEFAULT 0,
    battle_status    ENUM('NONE','ACTIVE','WON','LOST') NOT NULL DEFAULT 'NONE',
    visibility       ENUM('PUBLIC','PRIVATE','INVITE_ONLY') NOT NULL DEFAULT 'PUBLIC',
    agreed_to_terms  BOOLEAN         NOT NULL DEFAULT FALSE,
    terms_agreed_at  TIMESTAMP       NULL,
    is_plagiarised   BOOLEAN         NOT NULL DEFAULT FALSE,
    created_at       TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_ideas_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- -------------------------------------------------------
-- 3. votes
-- -------------------------------------------------------
CREATE TABLE IF NOT EXISTS votes (
    id         BIGINT    NOT NULL AUTO_INCREMENT,
    idea_id    BIGINT    NOT NULL,
    user_id    BIGINT    NOT NULL,
    type       ENUM('UP','DOWN') NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY one_vote_per_idea (idea_id, user_id),
    CONSTRAINT fk_votes_idea FOREIGN KEY (idea_id) REFERENCES ideas(id) ON DELETE CASCADE,
    CONSTRAINT fk_votes_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- -------------------------------------------------------
-- 4. comments
-- -------------------------------------------------------
CREATE TABLE IF NOT EXISTS comments (
    id         BIGINT    NOT NULL AUTO_INCREMENT,
    idea_id    BIGINT    NOT NULL,
    user_id    BIGINT    NOT NULL,
    content    TEXT      NOT NULL,
    tag        ENUM('SUPPORT','QUESTION','CHALLENGE') NOT NULL DEFAULT 'QUESTION',
    answered   BOOLEAN   NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_comments_idea FOREIGN KEY (idea_id) REFERENCES ideas(id) ON DELETE CASCADE,
    CONSTRAINT fk_comments_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- -------------------------------------------------------
-- 5. battles
-- -------------------------------------------------------
CREATE TABLE IF NOT EXISTS battles (
    id          BIGINT    NOT NULL AUTO_INCREMENT,
    idea_a_id   BIGINT    NOT NULL,
    idea_b_id   BIGINT    NOT NULL,
    votes_a     INT       NOT NULL DEFAULT 0,
    votes_b     INT       NOT NULL DEFAULT 0,
    winner_id   BIGINT,
    ends_at     TIMESTAMP NOT NULL,
    status      ENUM('ACTIVE','COMPLETED') NOT NULL DEFAULT 'ACTIVE',
    PRIMARY KEY (id),
    CONSTRAINT fk_battles_idea_a FOREIGN KEY (idea_a_id) REFERENCES ideas(id) ON DELETE CASCADE,
    CONSTRAINT fk_battles_idea_b FOREIGN KEY (idea_b_id) REFERENCES ideas(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- -------------------------------------------------------
-- 6. badges
-- -------------------------------------------------------
CREATE TABLE IF NOT EXISTS badges (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    user_id    BIGINT       NOT NULL,
    type       VARCHAR(50)  NOT NULL,
    earned_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_badges_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- -------------------------------------------------------
-- 7. hall_of_fame
-- -------------------------------------------------------
CREATE TABLE IF NOT EXISTS hall_of_fame (
    id             BIGINT          NOT NULL AUTO_INCREMENT,
    idea_id        BIGINT          NOT NULL,
    week_start     DATE            NOT NULL,
    rank_position  INT             NOT NULL,
    snapshot_score DECIMAL(10,2)   NOT NULL DEFAULT 0.00,
    PRIMARY KEY (id),
    CONSTRAINT fk_hof_idea FOREIGN KEY (idea_id) REFERENCES ideas(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- -------------------------------------------------------
-- 8. idea_invites
-- -------------------------------------------------------
CREATE TABLE IF NOT EXISTS idea_invites (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    idea_id       BIGINT       NOT NULL,
    invited_by    BIGINT       NOT NULL,
    invite_token  VARCHAR(255) NOT NULL UNIQUE,
    expires_at    TIMESTAMP    NOT NULL,
    used          BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_invites_idea FOREIGN KEY (idea_id) REFERENCES ideas(id) ON DELETE CASCADE,
    CONSTRAINT fk_invites_user FOREIGN KEY (invited_by) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- -------------------------------------------------------
-- 9. collab_requests
-- -------------------------------------------------------
CREATE TABLE IF NOT EXISTS collab_requests (
    id           BIGINT    NOT NULL AUTO_INCREMENT,
    idea_id      BIGINT    NOT NULL,
    requester_id BIGINT    NOT NULL,
    owner_id     BIGINT    NOT NULL,
    message      TEXT,
    status       ENUM('PENDING','ACCEPTED','REJECTED') NOT NULL DEFAULT 'PENDING',
    created_at   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_collab_idea      FOREIGN KEY (idea_id)      REFERENCES ideas(id)  ON DELETE CASCADE,
    CONSTRAINT fk_collab_requester FOREIGN KEY (requester_id) REFERENCES users(id)  ON DELETE CASCADE,
    CONSTRAINT fk_collab_owner     FOREIGN KEY (owner_id)     REFERENCES users(id)  ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- -------------------------------------------------------
-- 10. plagiarism_reports
-- -------------------------------------------------------
CREATE TABLE IF NOT EXISTS plagiarism_reports (
    id               BIGINT    NOT NULL AUTO_INCREMENT,
    reported_idea_id BIGINT    NOT NULL,
    original_idea_id BIGINT    NOT NULL,
    reported_by      BIGINT    NOT NULL,
    reason           TEXT,
    status           ENUM('PENDING','CONFIRMED','DISMISSED') NOT NULL DEFAULT 'PENDING',
    created_at       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_pr_reported  FOREIGN KEY (reported_idea_id) REFERENCES ideas(id) ON DELETE CASCADE,
    CONSTRAINT fk_pr_original  FOREIGN KEY (original_idea_id) REFERENCES ideas(id) ON DELETE CASCADE,
    CONSTRAINT fk_pr_reporter  FOREIGN KEY (reported_by)      REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- -------------------------------------------------------
-- 11. idea_certificates
-- -------------------------------------------------------
CREATE TABLE IF NOT EXISTS idea_certificates (
    id               BIGINT       NOT NULL AUTO_INCREMENT,
    idea_id          BIGINT       NOT NULL UNIQUE,
    certificate_code VARCHAR(100) NOT NULL UNIQUE,
    issued_at        TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_cert_idea FOREIGN KEY (idea_id) REFERENCES ideas(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
