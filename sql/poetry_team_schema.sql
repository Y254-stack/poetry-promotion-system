SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

USE `poetry_clean`;

-- =========================================================
-- poetry_team_schema.sql
-- Team-ready business schema for the poetry promotion system.
-- This script is additive and keeps existing content tables.
-- Existing content base:
--   author
--   poetry_work
--   work_sentence
--   work_relation
--   daily_recommendation
--   daily_recommend_item
-- =========================================================

-- ---------------------------------------------------------
-- 1. Content extension: tags
-- ---------------------------------------------------------

CREATE TABLE IF NOT EXISTS `tag` (
    `tag_id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `tag_name` VARCHAR(64) NOT NULL,
    `tag_type` VARCHAR(32) NOT NULL DEFAULT 'custom',
    `description` VARCHAR(255) DEFAULT NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`tag_id`),
    UNIQUE KEY `uk_tag_name_type` (`tag_name`, `tag_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Tag dictionary table';

CREATE TABLE IF NOT EXISTS `work_tag` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `work_id` BIGINT UNSIGNED NOT NULL,
    `tag_id` BIGINT UNSIGNED NOT NULL,
    `weight` DECIMAL(6,3) NOT NULL DEFAULT 1.000,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_work_tag` (`work_id`, `tag_id`),
    KEY `idx_work_tag_tag` (`tag_id`),
    CONSTRAINT `fk_work_tag_work`
        FOREIGN KEY (`work_id`) REFERENCES `poetry_work` (`work_id`) ON DELETE CASCADE,
    CONSTRAINT `fk_work_tag_tag`
        FOREIGN KEY (`tag_id`) REFERENCES `tag` (`tag_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Work-tag relation table';

-- ---------------------------------------------------------
-- 2. User and auth
-- ---------------------------------------------------------

CREATE TABLE IF NOT EXISTS `app_user` (
    `user_id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `username` VARCHAR(64) NOT NULL,
    `password_hash` VARCHAR(255) NOT NULL,
    `nickname` VARCHAR(64) NOT NULL,
    `real_name` VARCHAR(64) DEFAULT NULL,
    `email` VARCHAR(128) DEFAULT NULL,
    `phone` VARCHAR(32) DEFAULT NULL,
    `avatar_url` VARCHAR(512) DEFAULT NULL,
    `bio` VARCHAR(500) DEFAULT NULL,
    `gender` TINYINT UNSIGNED NOT NULL DEFAULT 0,
    `birthday` DATE DEFAULT NULL,
    `status` TINYINT UNSIGNED NOT NULL DEFAULT 1,
    `login_locked_until` DATETIME NULL DEFAULT NULL,
    `failed_login_count` INT UNSIGNED NOT NULL DEFAULT 0,
    `failed_login_window_start` DATETIME NULL DEFAULT NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`user_id`),
    UNIQUE KEY `uk_app_user_username` (`username`),
    UNIQUE KEY `uk_app_user_email` (`email`),
    UNIQUE KEY `uk_app_user_phone` (`phone`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Application users';

CREATE TABLE IF NOT EXISTS `user_login_log` (
    `log_id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT UNSIGNED NOT NULL,
    `login_ip` VARCHAR(64) DEFAULT NULL,
    `user_agent` VARCHAR(500) DEFAULT NULL,
    `login_status` VARCHAR(32) NOT NULL DEFAULT 'success',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`log_id`),
    KEY `idx_user_login_log_user` (`user_id`),
    CONSTRAINT `fk_user_login_log_user`
        FOREIGN KEY (`user_id`) REFERENCES `app_user` (`user_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Login log table';

-- ---------------------------------------------------------
-- 3. Favorites and follows
-- ---------------------------------------------------------

CREATE TABLE IF NOT EXISTS `user_favorite_work` (
    `favorite_id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT UNSIGNED NOT NULL,
    `work_id` BIGINT UNSIGNED NOT NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`favorite_id`),
    UNIQUE KEY `uk_user_favorite_work` (`user_id`, `work_id`),
    KEY `idx_user_favorite_work_work` (`work_id`),
    CONSTRAINT `fk_user_favorite_work_user`
        FOREIGN KEY (`user_id`) REFERENCES `app_user` (`user_id`) ON DELETE CASCADE,
    CONSTRAINT `fk_user_favorite_work_work`
        FOREIGN KEY (`work_id`) REFERENCES `poetry_work` (`work_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='User favorite works';

CREATE TABLE IF NOT EXISTS `user_favorite_author` (
    `favorite_id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT UNSIGNED NOT NULL,
    `author_id` BIGINT UNSIGNED NOT NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`favorite_id`),
    UNIQUE KEY `uk_user_favorite_author` (`user_id`, `author_id`),
    KEY `idx_user_favorite_author_author` (`author_id`),
    CONSTRAINT `fk_user_favorite_author_user`
        FOREIGN KEY (`user_id`) REFERENCES `app_user` (`user_id`) ON DELETE CASCADE,
    CONSTRAINT `fk_user_favorite_author_author`
        FOREIGN KEY (`author_id`) REFERENCES `author` (`author_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='User favorite authors';

CREATE TABLE IF NOT EXISTS `user_follow_user` (
    `follow_id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT UNSIGNED NOT NULL,
    `followed_user_id` BIGINT UNSIGNED NOT NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`follow_id`),
    UNIQUE KEY `uk_user_follow_user` (`user_id`, `followed_user_id`),
    KEY `idx_user_follow_user_followed` (`followed_user_id`),
    CONSTRAINT `fk_user_follow_user_user`
        FOREIGN KEY (`user_id`) REFERENCES `app_user` (`user_id`) ON DELETE CASCADE,
    CONSTRAINT `fk_user_follow_user_followed`
        FOREIGN KEY (`followed_user_id`) REFERENCES `app_user` (`user_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='User follows user';

CREATE TABLE IF NOT EXISTS `user_follow_author` (
    `follow_id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT UNSIGNED NOT NULL,
    `author_id` BIGINT UNSIGNED NOT NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`follow_id`),
    UNIQUE KEY `uk_user_follow_author` (`user_id`, `author_id`),
    KEY `idx_user_follow_author_author` (`author_id`),
    CONSTRAINT `fk_user_follow_author_user`
        FOREIGN KEY (`user_id`) REFERENCES `app_user` (`user_id`) ON DELETE CASCADE,
    CONSTRAINT `fk_user_follow_author_author`
        FOREIGN KEY (`author_id`) REFERENCES `author` (`author_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='User follows authors';

-- ---------------------------------------------------------
-- 4. Learning
-- ---------------------------------------------------------

CREATE TABLE IF NOT EXISTS `user_checkin` (
    `checkin_id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT UNSIGNED NOT NULL,
    `checkin_date` DATE NOT NULL,
    `streak_days` INT UNSIGNED NOT NULL DEFAULT 1,
    `reward_points` INT UNSIGNED NOT NULL DEFAULT 0,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`checkin_id`),
    UNIQUE KEY `uk_user_checkin` (`user_id`, `checkin_date`),
    CONSTRAINT `fk_user_checkin_user`
        FOREIGN KEY (`user_id`) REFERENCES `app_user` (`user_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Check-in records';

CREATE TABLE IF NOT EXISTS `study_plan` (
    `plan_id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT UNSIGNED NOT NULL,
    `plan_name` VARCHAR(128) NOT NULL,
    `plan_type` VARCHAR(32) NOT NULL DEFAULT 'custom',
    `daily_target` INT UNSIGNED NOT NULL DEFAULT 1,
    `start_date` DATE DEFAULT NULL,
    `end_date` DATE DEFAULT NULL,
    `status` VARCHAR(32) NOT NULL DEFAULT 'active',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`plan_id`),
    KEY `idx_study_plan_user` (`user_id`),
    CONSTRAINT `fk_study_plan_user`
        FOREIGN KEY (`user_id`) REFERENCES `app_user` (`user_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Study plan header';

CREATE TABLE IF NOT EXISTS `study_plan_item` (
    `plan_item_id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `plan_id` BIGINT UNSIGNED NOT NULL,
    `work_id` BIGINT UNSIGNED NOT NULL,
    `sort_no` INT UNSIGNED NOT NULL DEFAULT 1,
    `is_completed` TINYINT(1) NOT NULL DEFAULT 0,
    `completed_at` DATETIME DEFAULT NULL,
    PRIMARY KEY (`plan_item_id`),
    UNIQUE KEY `uk_study_plan_item` (`plan_id`, `work_id`),
    KEY `idx_study_plan_item_work` (`work_id`),
    CONSTRAINT `fk_study_plan_item_plan`
        FOREIGN KEY (`plan_id`) REFERENCES `study_plan` (`plan_id`) ON DELETE CASCADE,
    CONSTRAINT `fk_study_plan_item_work`
        FOREIGN KEY (`work_id`) REFERENCES `poetry_work` (`work_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Study plan items';

CREATE TABLE IF NOT EXISTS `study_record` (
    `record_id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT UNSIGNED NOT NULL,
    `work_id` BIGINT UNSIGNED DEFAULT NULL,
    `action_type` VARCHAR(32) NOT NULL COMMENT 'view, study, recite, search',
    `duration_seconds` INT UNSIGNED NOT NULL DEFAULT 0,
    `progress_ratio` DECIMAL(5,2) NOT NULL DEFAULT 0.00,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`record_id`),
    KEY `idx_study_record_user_action` (`user_id`, `action_type`),
    KEY `idx_study_record_work` (`work_id`),
    CONSTRAINT `fk_study_record_user`
        FOREIGN KEY (`user_id`) REFERENCES `app_user` (`user_id`) ON DELETE CASCADE,
    CONSTRAINT `fk_study_record_work`
        FOREIGN KEY (`work_id`) REFERENCES `poetry_work` (`work_id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Study behavior records';

CREATE TABLE IF NOT EXISTS `review_plan` (
    `review_plan_id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT UNSIGNED NOT NULL,
    `work_id` BIGINT UNSIGNED NOT NULL,
    `next_review_at` DATETIME NOT NULL,
    `interval_days` INT UNSIGNED NOT NULL DEFAULT 1,
    `ease_factor` DECIMAL(5,2) NOT NULL DEFAULT 2.50,
    `review_count` INT UNSIGNED NOT NULL DEFAULT 0,
    `status` VARCHAR(32) NOT NULL DEFAULT 'active',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`review_plan_id`),
    UNIQUE KEY `uk_review_plan_user_work` (`user_id`, `work_id`),
    KEY `idx_review_plan_next_review_at` (`next_review_at`),
    CONSTRAINT `fk_review_plan_user`
        FOREIGN KEY (`user_id`) REFERENCES `app_user` (`user_id`) ON DELETE CASCADE,
    CONSTRAINT `fk_review_plan_work`
        FOREIGN KEY (`work_id`) REFERENCES `poetry_work` (`work_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Review plan for spaced repetition';

CREATE TABLE IF NOT EXISTS `review_log` (
    `review_log_id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT UNSIGNED NOT NULL,
    `work_id` BIGINT UNSIGNED NOT NULL,
    `reviewed_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `result` VARCHAR(32) NOT NULL COMMENT 'again, hard, good, easy',
    `score` DECIMAL(5,2) DEFAULT NULL,
    `duration_seconds` INT UNSIGNED NOT NULL DEFAULT 0,
    PRIMARY KEY (`review_log_id`),
    KEY `idx_review_log_user` (`user_id`),
    KEY `idx_review_log_work` (`work_id`),
    CONSTRAINT `fk_review_log_user`
        FOREIGN KEY (`user_id`) REFERENCES `app_user` (`user_id`) ON DELETE CASCADE,
    CONSTRAINT `fk_review_log_work`
        FOREIGN KEY (`work_id`) REFERENCES `poetry_work` (`work_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Review execution log';

CREATE TABLE IF NOT EXISTS `quiz_record` (
    `quiz_record_id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT UNSIGNED NOT NULL,
    `work_id` BIGINT UNSIGNED DEFAULT NULL,
    `sentence_id` BIGINT UNSIGNED DEFAULT NULL,
    `quiz_type` VARCHAR(32) NOT NULL COMMENT 'fill_blank, chain, feihua, quiz',
    `difficulty_level` TINYINT UNSIGNED NOT NULL DEFAULT 1,
    `question_payload` JSON NOT NULL,
    `answer_payload` JSON DEFAULT NULL,
    `correct_payload` JSON DEFAULT NULL,
    `is_correct` TINYINT(1) NOT NULL DEFAULT 0,
    `score` DECIMAL(6,2) NOT NULL DEFAULT 0.00,
    `duration_seconds` INT UNSIGNED NOT NULL DEFAULT 0,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`quiz_record_id`),
    KEY `idx_quiz_record_user_type` (`user_id`, `quiz_type`),
    KEY `idx_quiz_record_work` (`work_id`),
    KEY `idx_quiz_record_sentence` (`sentence_id`),
    CONSTRAINT `fk_quiz_record_user`
        FOREIGN KEY (`user_id`) REFERENCES `app_user` (`user_id`) ON DELETE CASCADE,
    CONSTRAINT `fk_quiz_record_work`
        FOREIGN KEY (`work_id`) REFERENCES `poetry_work` (`work_id`) ON DELETE SET NULL,
    CONSTRAINT `fk_quiz_record_sentence`
        FOREIGN KEY (`sentence_id`) REFERENCES `work_sentence` (`sentence_id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Quiz records';

-- ---------------------------------------------------------
-- 5. Creation and community
-- ---------------------------------------------------------

CREATE TABLE IF NOT EXISTS `creative_card` (
    `card_id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT UNSIGNED NOT NULL,
    `work_id` BIGINT UNSIGNED DEFAULT NULL,
    `card_title` VARCHAR(255) DEFAULT NULL,
    `quote_text` VARCHAR(500) DEFAULT NULL,
    `theme_name` VARCHAR(64) DEFAULT NULL,
    `cover_url` VARCHAR(512) DEFAULT NULL,
    `content_json` JSON NOT NULL,
    `status` VARCHAR(32) NOT NULL DEFAULT 'draft',
    `is_public` TINYINT(1) NOT NULL DEFAULT 0,
    `like_count` INT UNSIGNED NOT NULL DEFAULT 0,
    `use_count` INT UNSIGNED NOT NULL DEFAULT 0,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`card_id`),
    KEY `idx_creative_card_user` (`user_id`),
    KEY `idx_creative_card_work` (`work_id`),
    CONSTRAINT `fk_creative_card_user`
        FOREIGN KEY (`user_id`) REFERENCES `app_user` (`user_id`) ON DELETE CASCADE,
    CONSTRAINT `fk_creative_card_work`
        FOREIGN KEY (`work_id`) REFERENCES `poetry_work` (`work_id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Creative cards';

CREATE TABLE IF NOT EXISTS `community_post` (
    `post_id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT UNSIGNED NOT NULL,
    `post_type` VARCHAR(32) NOT NULL DEFAULT 'text',
    `title` VARCHAR(255) DEFAULT NULL,
    `content_text` MEDIUMTEXT DEFAULT NULL,
    `topic_tag` VARCHAR(64) DEFAULT NULL,
    `related_work_id` BIGINT UNSIGNED DEFAULT NULL,
    `related_author_id` BIGINT UNSIGNED DEFAULT NULL,
    `related_card_id` BIGINT UNSIGNED DEFAULT NULL,
    `status` VARCHAR(32) NOT NULL DEFAULT 'published',
    `view_count` INT UNSIGNED NOT NULL DEFAULT 0,
    `like_count` INT UNSIGNED NOT NULL DEFAULT 0,
    `comment_count` INT UNSIGNED NOT NULL DEFAULT 0,
    `collect_count` INT UNSIGNED NOT NULL DEFAULT 0,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`post_id`),
    KEY `idx_community_post_user` (`user_id`),
    KEY `idx_community_post_status` (`status`),
    KEY `idx_community_post_work` (`related_work_id`),
    CONSTRAINT `fk_community_post_user`
        FOREIGN KEY (`user_id`) REFERENCES `app_user` (`user_id`) ON DELETE CASCADE,
    CONSTRAINT `fk_community_post_work`
        FOREIGN KEY (`related_work_id`) REFERENCES `poetry_work` (`work_id`) ON DELETE SET NULL,
    CONSTRAINT `fk_community_post_author`
        FOREIGN KEY (`related_author_id`) REFERENCES `author` (`author_id`) ON DELETE SET NULL,
    CONSTRAINT `fk_community_post_card`
        FOREIGN KEY (`related_card_id`) REFERENCES `creative_card` (`card_id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Community posts';

CREATE TABLE IF NOT EXISTS `community_comment` (
    `comment_id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `post_id` BIGINT UNSIGNED NOT NULL,
    `user_id` BIGINT UNSIGNED NOT NULL,
    `parent_comment_id` BIGINT UNSIGNED DEFAULT NULL,
    `reply_user_id` BIGINT UNSIGNED DEFAULT NULL,
    `content_text` VARCHAR(1000) NOT NULL,
    `status` VARCHAR(32) NOT NULL DEFAULT 'published',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`comment_id`),
    KEY `idx_community_comment_post` (`post_id`),
    KEY `idx_community_comment_user` (`user_id`),
    KEY `idx_community_comment_parent` (`parent_comment_id`),
    CONSTRAINT `fk_community_comment_post`
        FOREIGN KEY (`post_id`) REFERENCES `community_post` (`post_id`) ON DELETE CASCADE,
    CONSTRAINT `fk_community_comment_user`
        FOREIGN KEY (`user_id`) REFERENCES `app_user` (`user_id`) ON DELETE CASCADE,
    CONSTRAINT `fk_community_comment_parent`
        FOREIGN KEY (`parent_comment_id`) REFERENCES `community_comment` (`comment_id`) ON DELETE CASCADE,
    CONSTRAINT `fk_community_comment_reply_user`
        FOREIGN KEY (`reply_user_id`) REFERENCES `app_user` (`user_id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Community comments';

CREATE TABLE IF NOT EXISTS `community_post_like` (
    `like_id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `post_id` BIGINT UNSIGNED NOT NULL,
    `user_id` BIGINT UNSIGNED NOT NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`like_id`),
    UNIQUE KEY `uk_community_post_like` (`post_id`, `user_id`),
    KEY `idx_community_post_like_user` (`user_id`),
    CONSTRAINT `fk_community_post_like_post`
        FOREIGN KEY (`post_id`) REFERENCES `community_post` (`post_id`) ON DELETE CASCADE,
    CONSTRAINT `fk_community_post_like_user`
        FOREIGN KEY (`user_id`) REFERENCES `app_user` (`user_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Community post likes';

CREATE TABLE IF NOT EXISTS `community_post_collect` (
    `collect_id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `post_id` BIGINT UNSIGNED NOT NULL,
    `user_id` BIGINT UNSIGNED NOT NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`collect_id`),
    UNIQUE KEY `uk_community_post_collect` (`post_id`, `user_id`),
    KEY `idx_community_post_collect_user` (`user_id`),
    CONSTRAINT `fk_community_post_collect_post`
        FOREIGN KEY (`post_id`) REFERENCES `community_post` (`post_id`) ON DELETE CASCADE,
    CONSTRAINT `fk_community_post_collect_user`
        FOREIGN KEY (`user_id`) REFERENCES `app_user` (`user_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Community post collections';

CREATE TABLE IF NOT EXISTS `user_notification` (
    `notification_id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT UNSIGNED NOT NULL,
    `biz_type` VARCHAR(32) NOT NULL DEFAULT 'system',
    `title` VARCHAR(255) NOT NULL,
    `content_text` VARCHAR(2000) NOT NULL,
    `related_type` VARCHAR(32) DEFAULT NULL,
    `related_id` BIGINT UNSIGNED DEFAULT NULL,
    `is_read` TINYINT(1) NOT NULL DEFAULT 0,
    `read_at` DATETIME DEFAULT NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`notification_id`),
    KEY `idx_user_notification_user` (`user_id`, `is_read`),
    CONSTRAINT `fk_user_notification_user`
        FOREIGN KEY (`user_id`) REFERENCES `app_user` (`user_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='User notifications';

-- ---------------------------------------------------------
-- 6. API-oriented views for backend modules
-- ---------------------------------------------------------

DROP VIEW IF EXISTS `v_api_daily_recommend`;
CREATE VIEW `v_api_daily_recommend` AS
SELECT
    r.`recommend_date`,
    r.`theme_name`,
    r.`intro_text`,
    i.`sort_no`,
    w.`work_id`,
    w.`title`,
    w.`author_name_cache` AS `author_name`,
    w.`dynasty_name`,
    i.`reason_type`,
    i.`reason_text`
FROM `daily_recommendation` r
JOIN `daily_recommend_item` i
    ON i.`recommendation_id` = r.`recommendation_id`
JOIN `poetry_work` w
    ON w.`work_id` = i.`work_id`;

DROP VIEW IF EXISTS `v_api_related_work`;
CREATE VIEW `v_api_related_work` AS
SELECT
    r.`work_id`,
    r.`related_work_id`,
    r.`relation_type`,
    r.`score`,
    w.`title`,
    w.`author_name_cache` AS `author_name`,
    w.`dynasty_name`,
    LEFT(w.`content_text`, 120) AS `content_preview`
FROM `work_relation` r
JOIN `poetry_work` w
    ON w.`work_id` = r.`related_work_id`;

SET FOREIGN_KEY_CHECKS = 1;

-- Suggested checks:
-- SHOW TABLES;
-- SELECT * FROM v_api_daily_recommend LIMIT 10;
-- SELECT * FROM v_api_related_work WHERE work_id = 1 ORDER BY score DESC LIMIT 10;
