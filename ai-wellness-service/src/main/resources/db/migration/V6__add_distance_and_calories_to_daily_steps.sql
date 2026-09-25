-- ===========================================================================
-- Flyway Database Migration: V6__add_distance_and_calories_to_daily_steps.sql
--
-- Adds distance_meters and calories_burned columns to daily_steps table.
-- ===========================================================================

ALTER TABLE daily_steps ADD COLUMN distance_meters DOUBLE DEFAULT 0.0;
ALTER TABLE daily_steps ADD COLUMN calories_burned DOUBLE DEFAULT 0.0;
