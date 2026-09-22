-- ===========================================================================
-- Flyway Database Migration: V4__remove_dummy_user1.sql
--
-- Removes legacy seed user 'user1' ('User One') and all associated data
-- to ensure only genuine authenticated users appear on leaderboards and UI.
-- ===========================================================================

-- 1. Remove associated activity states
DELETE FROM user_activity_states WHERE user_id IN (SELECT id FROM users WHERE email LIKE '%user1%' OR full_name = 'User One');

-- 2. Remove associated daily steps
DELETE FROM daily_steps WHERE user_id IN (SELECT id FROM users WHERE email LIKE '%user1%' OR full_name = 'User One');

-- 3. Remove associated physical activities
DELETE FROM activities WHERE user_id IN (SELECT id FROM users WHERE email LIKE '%user1%' OR full_name = 'User One');

-- 4. Remove associated exercises
DELETE FROM exercises WHERE user_id IN (SELECT id FROM users WHERE email LIKE '%user1%' OR full_name = 'User One');

-- 5. Remove team memberships
DELETE FROM team_members WHERE user_id IN (SELECT id FROM users WHERE email LIKE '%user1%' OR full_name = 'User One');

-- 6. Remove challenge memberships
DELETE FROM challenge_members WHERE user_id IN (SELECT id FROM users WHERE email LIKE '%user1%' OR full_name = 'User One');

-- 7. Remove AI conversations and messages
DELETE FROM ai_messages WHERE conversation_id IN (
    SELECT id FROM ai_conversations WHERE user_id IN (SELECT id FROM users WHERE email LIKE '%user1%' OR full_name = 'User One')
);
DELETE FROM ai_conversations WHERE user_id IN (SELECT id FROM users WHERE email LIKE '%user1%' OR full_name = 'User One');

-- 8. Remove teams owned by dummy user
DELETE FROM teams WHERE owner_id IN (SELECT id FROM users WHERE email LIKE '%user1%' OR full_name = 'User One');

-- 9. Remove challenges created by dummy user
DELETE FROM challenges WHERE created_by IN (SELECT id FROM users WHERE email LIKE '%user1%' OR full_name = 'User One');

-- 10. Finally, delete the dummy user(s)
DELETE FROM users WHERE email LIKE '%user1%' OR full_name = 'User One';
