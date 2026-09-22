-- ===========================================================================
-- Flyway Database Migration: V3__user_activity_state.sql
--
-- Adds user_activity_states table for persistent inactivity detection
-- ===========================================================================

CREATE TABLE IF NOT EXISTS user_activity_states (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    last_activity_time TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    last_step_count BIGINT NOT NULL DEFAULT 0,
    notification_sent BOOLEAN NOT NULL DEFAULT FALSE,
    notification_sent_at TIMESTAMP(6) NULL,
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_uas_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
