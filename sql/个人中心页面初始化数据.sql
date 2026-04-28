

-- Demo seed data for User Center:
-- - user follow list (user_follow_user)
-- - favorite poems (user_favorite_work)
-- - community posts + collects (community_post, community_post_collect)
--
-- This script assumes schema: poetry_clean (see backend application.yml)

USE `poetry_clean`;

-- Optional: seed data for a newly registered "smoke" user (used by scripts/smoke_user_center.ps1)
SET @seed_username := 'smoke_user_2026';
SET @u_seed := (SELECT user_id FROM app_user WHERE username = @seed_username LIMIT 1);

-- Resolve demo users (based on existing seeded data)
SET @u1 := (SELECT user_id FROM app_user WHERE username = 'testuser' LIMIT 1);
SET @u2 := (SELECT user_id FROM app_user WHERE username = 'momo' LIMIT 1);
SET @u3 := (SELECT user_id FROM app_user WHERE username = 'zhouziyu' LIMIT 1);
SET @u4 := (SELECT user_id FROM app_user WHERE username = 'u_demo_002' LIMIT 1);

-- Fallback to "first users" if usernames don't exist
SET @u1 := COALESCE(@u1, (SELECT user_id FROM app_user ORDER BY user_id ASC LIMIT 1));
SET @u2 := COALESCE(@u2, (SELECT user_id FROM app_user ORDER BY user_id ASC LIMIT 1 OFFSET 1));
SET @u3 := COALESCE(@u3, (SELECT user_id FROM app_user ORDER BY user_id ASC LIMIT 1 OFFSET 2));
SET @u4 := COALESCE(@u4, (SELECT user_id FROM app_user ORDER BY user_id ASC LIMIT 1 OFFSET 3));

-- Resolve some existing poems (works)
SET @w1 := (SELECT work_id FROM poetry_work ORDER BY work_id ASC LIMIT 1);
SET @w2 := (SELECT work_id FROM poetry_work ORDER BY work_id ASC LIMIT 1 OFFSET 1);
SET @w3 := (SELECT work_id FROM poetry_work ORDER BY work_id ASC LIMIT 1 OFFSET 2);
SET @w4 := (SELECT work_id FROM poetry_work ORDER BY work_id ASC LIMIT 1 OFFSET 3);

-- ---------------------------------------------------------
-- 1) Seed follows
-- ---------------------------------------------------------
INSERT INTO user_follow_user(user_id, followed_user_id)
SELECT @u1, @u2 WHERE @u1 IS NOT NULL AND @u2 IS NOT NULL
ON DUPLICATE KEY UPDATE created_at = created_at;

INSERT INTO user_follow_user(user_id, followed_user_id)
SELECT @u1, @u3 WHERE @u1 IS NOT NULL AND @u3 IS NOT NULL
ON DUPLICATE KEY UPDATE created_at = created_at;

INSERT INTO user_follow_user(user_id, followed_user_id)
SELECT @u2, @u3 WHERE @u2 IS NOT NULL AND @u3 IS NOT NULL
ON DUPLICATE KEY UPDATE created_at = created_at;

-- For smoke user: follow u2 and u3 so the list is non-empty
INSERT INTO user_follow_user(user_id, followed_user_id)
SELECT @u_seed, @u2 WHERE @u_seed IS NOT NULL AND @u2 IS NOT NULL
ON DUPLICATE KEY UPDATE created_at = created_at;

INSERT INTO user_follow_user(user_id, followed_user_id)
SELECT @u_seed, @u3 WHERE @u_seed IS NOT NULL AND @u3 IS NOT NULL
ON DUPLICATE KEY UPDATE created_at = created_at;

-- ---------------------------------------------------------
-- 2) Seed favorite poems
-- ---------------------------------------------------------
INSERT INTO user_favorite_work(user_id, work_id)
SELECT @u1, @w1 WHERE @u1 IS NOT NULL AND @w1 IS NOT NULL
ON DUPLICATE KEY UPDATE created_at = created_at;

INSERT INTO user_favorite_work(user_id, work_id)
SELECT @u1, @w2 WHERE @u1 IS NOT NULL AND @w2 IS NOT NULL
ON DUPLICATE KEY UPDATE created_at = created_at;

INSERT INTO user_favorite_work(user_id, work_id)
SELECT @u2, @w3 WHERE @u2 IS NOT NULL AND @w3 IS NOT NULL
ON DUPLICATE KEY UPDATE created_at = created_at;

INSERT INTO user_favorite_work(user_id, work_id)
SELECT @u3, @w4 WHERE @u3 IS NOT NULL AND @w4 IS NOT NULL
ON DUPLICATE KEY UPDATE created_at = created_at;

-- For smoke user: favorite a couple of poems
INSERT INTO user_favorite_work(user_id, work_id)
SELECT @u_seed, @w1 WHERE @u_seed IS NOT NULL AND @w1 IS NOT NULL
ON DUPLICATE KEY UPDATE created_at = created_at;

INSERT INTO user_favorite_work(user_id, work_id)
SELECT @u_seed, @w2 WHERE @u_seed IS NOT NULL AND @w2 IS NOT NULL
ON DUPLICATE KEY UPDATE created_at = created_at;

-- ---------------------------------------------------------
-- 3) Seed community posts (deterministic post_id)
-- ---------------------------------------------------------
INSERT INTO community_post(
    post_id, user_id, post_type, title, content_text, topic_tag, related_work_id, status,
    view_count, like_count, comment_count, collect_count
)
SELECT
    1001, @u2, 'text',
    '今天读到一首很喜欢的诗',
    CONCAT('分享一下最近反复读的一首诗：', COALESCE((SELECT title FROM poetry_work WHERE work_id = @w1), '（未知）'),
           '。你们最喜欢的句子是哪一句？'),
    '读诗打卡', @w1, 'published',
    12, 1, 0, 0
WHERE @u2 IS NOT NULL
ON DUPLICATE KEY UPDATE updated_at = CURRENT_TIMESTAMP;

INSERT INTO community_post(
    post_id, user_id, post_type, title, content_text, topic_tag, related_work_id, status,
    view_count, like_count, comment_count, collect_count
)
SELECT
    1002, @u3, 'text',
    '写作练习：仿写一句',
    '我尝试用“对仗/意象”做一个仿写练习，欢迎指正与补充。',
    '创作练习', @w2, 'published',
    35, 3, 1, 0
WHERE @u3 IS NOT NULL
ON DUPLICATE KEY UPDATE updated_at = CURRENT_TIMESTAMP;

INSERT INTO community_post(
    post_id, user_id, post_type, title, content_text, topic_tag, related_work_id, status,
    view_count, like_count, comment_count, collect_count
)
SELECT
    1003, @u4, 'text',
    '这首诗的注释你们怎么看？',
    '同一首诗不同注释版本差异挺大，大家更认可哪种解释？',
    '注释讨论', @w3, 'published',
    7, 0, 0, 0
WHERE @u4 IS NOT NULL
ON DUPLICATE KEY UPDATE updated_at = CURRENT_TIMESTAMP;

-- ---------------------------------------------------------
-- 4) Seed post collects (favorite posts)
-- ---------------------------------------------------------
INSERT INTO community_post_collect(post_id, user_id)
SELECT 1001, @u1 WHERE @u1 IS NOT NULL
ON DUPLICATE KEY UPDATE created_at = created_at;

INSERT INTO community_post_collect(post_id, user_id)
SELECT 1002, @u1 WHERE @u1 IS NOT NULL
ON DUPLICATE KEY UPDATE created_at = created_at;

INSERT INTO community_post_collect(post_id, user_id)
SELECT 1002, @u2 WHERE @u2 IS NOT NULL
ON DUPLICATE KEY UPDATE created_at = created_at;

-- For smoke user: collect a couple of posts
INSERT INTO community_post_collect(post_id, user_id)
SELECT 1001, @u_seed WHERE @u_seed IS NOT NULL
ON DUPLICATE KEY UPDATE created_at = created_at;

INSERT INTO community_post_collect(post_id, user_id)
SELECT 1002, @u_seed WHERE @u_seed IS NOT NULL
ON DUPLICATE KEY UPDATE created_at = created_at;

-- Sync collect_count for the seeded posts
UPDATE community_post p
SET p.collect_count = (
    SELECT COUNT(*)
    FROM community_post_collect c
    WHERE c.post_id = p.post_id
)
WHERE p.post_id IN (1001, 1002, 1003);

