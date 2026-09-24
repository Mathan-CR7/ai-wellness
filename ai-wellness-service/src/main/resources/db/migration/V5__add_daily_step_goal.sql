-- ===========================================================================
-- Flyway Database Migration: V5__add_daily_step_goal.sql
--
-- Adds daily_step_goal column to users table with default 10000.
-- Standard MySQL syntax compatible with all MySQL versions (5.7, 8.0+).
-- ===========================================================================

ALTER TABLE users ADD COLUMN daily_step_goal INT DEFAULT 10000;
