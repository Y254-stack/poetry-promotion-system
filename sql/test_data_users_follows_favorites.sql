-- =============================================================================
-- 联调测试数据：注册用户 + 关注 + 诗词收藏 + 帖子收藏 + 创作卡片 + 社区帖子
-- =============================================================================
-- 登录说明：
--   所有账号密码均为：123456
--   password_hash 为 BCrypt（与后端 BCryptPasswordEncoder 一致）
--
-- 执行前请确认：
--   1. 已创建库并已执行诗词基础数据（poetry_work、author 表中有数据）
--   2. 根据实际环境修改下面的 USE 数据库名
--   3. 若 app_user.user_id 与下列 810001–810005 冲突，请先调整 ID 区间
-- =============================================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- 与本地 poetry_team_schema / auth.sql 默认库名一致；按需修改
USE `poetry_clean`;

-- 密码均为 123456（BCrypt $2b$10$，Spring Security 可校验）
SET @pwd_123456 := '$2b$10$bRmtfga6wYBHhCobPcwClORoqkShWSungn3PtlrQJuYuTxHo1oUw.';

-- ---------------------------------------------------------------------------
-- 1. 注册用户（固定 user_id 便于脚本内引用）
-- ---------------------------------------------------------------------------
INSERT INTO `app_user` (`user_id`, `username`, `password_hash`, `nickname`, `email`, `status`)
VALUES
    (810001, 'test_poet_a', @pwd_123456, '诗词爱好者A', 'poet_a@test.local', 1),
    (810002, 'test_poet_b', @pwd_123456, '诗词爱好者B', 'poet_b@test.local', 1),
    (810003, 'test_reader_c', @pwd_123456, '读者小C', 'reader_c@test.local', 1),
    (810004, 'test_reader_d', @pwd_123456, '读者小D', 'reader_d@test.local', 1),
    (810005, 'test_ops_e', @pwd_123456, '运营小E', 'ops_e@test.local', 1)
ON DUPLICATE KEY UPDATE
    `password_hash` = VALUES(`password_hash`),
    `nickname`      = VALUES(`nickname`),
    `email`         = VALUES(`email`);

-- ---------------------------------------------------------------------------
-- 2. 用户互相关注（user_follow_user：user_id -> followed_user_id）
--    A/B 互关；C、D 关注 A；C 关注 B；E 关注 A
-- ---------------------------------------------------------------------------
INSERT IGNORE INTO `user_follow_user` (`user_id`, `followed_user_id`, `created_at`) VALUES
    (810002, 810001, DATE_SUB(NOW(), INTERVAL 10 DAY)),
    (810001, 810002, DATE_SUB(NOW(), INTERVAL 9 DAY)),
    (810003, 810001, DATE_SUB(NOW(), INTERVAL 7 DAY)),
    (810003, 810002, DATE_SUB(NOW(), INTERVAL 6 DAY)),
    (810004, 810001, DATE_SUB(NOW(), INTERVAL 5 DAY)),
    (810005, 810001, DATE_SUB(NOW(), INTERVAL 3 DAY));

-- ---------------------------------------------------------------------------
-- 3. 创作卡片（creative_card）：A 已发布公开卡；B 草稿卡
-- ---------------------------------------------------------------------------
INSERT INTO `creative_card` (
    `user_id`, `work_id`, `card_title`, `quote_text`, `theme_name`,
    `content_json`, `status`, `is_public`, `like_count`, `use_count`
)
SELECT
    810001,
    (SELECT w.`work_id` FROM `poetry_work` w ORDER BY w.`work_id` ASC LIMIT 1 OFFSET 0),
    '春日摘录卡片',
    '春风得意马蹄疾，一日看尽长安花。',
    '春景',
    CAST('{"layout":"quote_card","fontScale":1.0}' AS JSON),
    'published',
    1,
    3,
    1
WHERE EXISTS (SELECT 1 FROM `poetry_work` LIMIT 1);

INSERT INTO `creative_card` (
    `user_id`, `work_id`, `card_title`, `quote_text`, `theme_name`,
    `content_json`, `status`, `is_public`, `like_count`, `use_count`
)
SELECT
    810002,
    (SELECT w.`work_id` FROM `poetry_work` w ORDER BY w.`work_id` ASC LIMIT 1 OFFSET 1),
    '草稿：山水意境',
    '行到水穷处，坐看云起时。',
    '山水',
    CAST('{"layout":"draft","note":"待配图"}' AS JSON),
    'draft',
    0,
    0,
    0
WHERE EXISTS (SELECT 1 FROM `poetry_work` w ORDER BY w.`work_id` ASC LIMIT 1 OFFSET 1);

SET @card_a := (SELECT `card_id` FROM `creative_card` WHERE `user_id` = 810001 ORDER BY `card_id` DESC LIMIT 1);

-- ---------------------------------------------------------------------------
-- 4. 社区帖子（community_post）：A 两篇、B 一篇；均为 published
-- ---------------------------------------------------------------------------
INSERT INTO `community_post` (
    `user_id`, `post_type`, `title`, `content_text`, `topic_tag`,
    `related_work_id`, `related_card_id`, `status`,
    `view_count`, `like_count`, `comment_count`, `collect_count`
)
SELECT
    810001,
    'text',
    '你最喜欢的春日诗句？',
    '我先来：等闲识得东风面，万紫千红总是春。大家在评论区接龙吧。',
    '话题',
    (SELECT w.`work_id` FROM `poetry_work` w ORDER BY w.`work_id` ASC LIMIT 1),
    NULL,
    'published',
    12,
    2,
    0,
    1
WHERE EXISTS (SELECT 1 FROM `poetry_work` LIMIT 1);

INSERT INTO `community_post` (
    `user_id`, `post_type`, `title`, `content_text`, `topic_tag`,
    `related_work_id`, `related_card_id`, `status`,
    `view_count`, `like_count`, `comment_count`, `collect_count`
)
SELECT
    810001,
    'text',
    '分享一张刚做的摘录卡片',
    '把喜欢的句子做成卡片，排版用了默认主题，欢迎交流。',
    '创作',
    NULL,
    @card_a,
    'published',
    30,
    5,
    1,
    2
WHERE @card_a IS NOT NULL;

INSERT INTO `community_post` (
    `user_id`, `post_type`, `title`, `content_text`, `topic_tag`,
    `related_work_id`, `related_author_id`, `status`,
    `view_count`, `like_count`, `comment_count`, `collect_count`
)
SELECT
    810002,
    'text',
    '杜甫律诗精读打卡',
    '本周精读《登高》，从格律与意象两方面做笔记，有兴趣的可以一起。',
    '学习',
    NULL,
    (SELECT a.`author_id` FROM `author` a ORDER BY a.`author_id` ASC LIMIT 1),
    'published',
    8,
    1,
    0,
    0
WHERE EXISTS (SELECT 1 FROM `author` LIMIT 1);

-- ---------------------------------------------------------------------------
-- 5. 诗词收藏（后端接口表名：user_favorite_poem）
--    C 收藏 3 首；D 收藏 1 首（按 work_id 升序取前几条）
-- ---------------------------------------------------------------------------
INSERT IGNORE INTO `user_favorite_poem` (`user_id`, `work_id`, `created_at`)
SELECT 810003, w.`work_id`, DATE_SUB(NOW(), INTERVAL 4 DAY)
FROM `poetry_work` w
ORDER BY w.`work_id` ASC
LIMIT 3;

INSERT IGNORE INTO `user_favorite_poem` (`user_id`, `work_id`, `created_at`)
SELECT 810004, w.`work_id`, DATE_SUB(NOW(), INTERVAL 2 DAY)
FROM `poetry_work` w
ORDER BY w.`work_id` ASC
LIMIT 1;

-- ---------------------------------------------------------------------------
-- 6. 帖子收藏（community_post_collect）：D 收藏 A 的一篇帖子；E 收藏 B 的帖子
-- ---------------------------------------------------------------------------
INSERT IGNORE INTO `community_post_collect` (`post_id`, `user_id`, `created_at`)
SELECT p.`post_id`, 810004, DATE_SUB(NOW(), INTERVAL 1 DAY)
FROM `community_post` p
WHERE p.`user_id` = 810001 AND p.`status` = 'published'
ORDER BY p.`post_id` DESC
LIMIT 1;

INSERT IGNORE INTO `community_post_collect` (`post_id`, `user_id`, `created_at`)
SELECT p.`post_id`, 810005, NOW()
FROM `community_post` p
WHERE p.`user_id` = 810002 AND p.`status` = 'published'
ORDER BY p.`post_id` DESC
LIMIT 1;


-- =============================================================================
-- 账号一览（密码均为 123456）
--   test_poet_a   / 诗词爱好者A   — 有帖子、卡片，被多人关注
--   test_poet_b   / 诗词爱好者B   — 有帖子、草稿卡
--   test_reader_c / 读者小C     — 诗词收藏、关注 A/B
--   test_reader_d / 读者小D     — 诗词收藏、关注 A、帖子收藏
--   test_ops_e    / 运营小E     — 关注 A、帖子收藏
-- =============================================================================
