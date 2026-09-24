-- ===========================================================================
-- Flyway Database Migration: V5__add_daily_step_goal.sql
--
-- Adds the daily_step_goal column to the users table so users can configure
-- their personal step goal. Defaults to 10000 for all existing users.
-- ===========================================================================

ALTER TABLE users ADD COLUMN IF NOT EXISTS daily_step_goal INT DEFAULT 10000;

UPDATE users SET daily_step_goal = 10000 WHERE daily_step_goal IS NULL;
