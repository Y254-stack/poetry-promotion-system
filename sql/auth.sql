-- auth_schema.sql
-- Minimal schema for auth-related tables and sample data

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

CREATE DATABASE IF NOT EXISTS `poetry_clean` DEFAULT CHARACTER SET utf8mb4 DEFAULT COLLATE utf8mb4_unicode_ci;
USE `poetry_clean`;

-- users table (app_user)
CREATE TABLE IF NOT EXISTS `app_user` (
    `user_id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `username` VARCHAR(64) NOT NULL,
    `password_hash` VARCHAR(255) NOT NULL,
    `nickname` VARCHAR(64) NOT NULL,
    `email` VARCHAR(128) DEFAULT NULL,
    `status` TINYINT UNSIGNED NOT NULL DEFAULT 1,
    `login_locked_until` DATETIME NULL DEFAULT NULL,
    `failed_login_count` INT UNSIGNED NOT NULL DEFAULT 0,
    `failed_login_window_start` DATETIME NULL DEFAULT NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`user_id`),
    UNIQUE KEY `uk_app_user_username` (`username`),
    UNIQUE KEY `uk_app_user_email` (`email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Application users';

-- login log
CREATE TABLE IF NOT EXISTS `user_login_log` (
    `log_id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT UNSIGNED NOT NULL,
    `login_ip` VARCHAR(64) DEFAULT NULL,
    `user_agent` VARCHAR(500) DEFAULT NULL,
    `login_status` VARCHAR(32) NOT NULL DEFAULT 'success',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`log_id`),
    KEY `idx_user_login_log_user` (`user_id`),
    CONSTRAINT `fk_user_login_log_user` FOREIGN KEY (`user_id`) REFERENCES `app_user` (`user_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- sample user (password: 'password' hashed with BCrypt)
-- NOTE: Replace password_hash with an actual bcrypt hash when provisioning.
INSERT INTO `app_user` (`username`, `password_hash`, `nickname`, `email`)
VALUES ('testuser', '$2a$10$7EqJtq98hPqEX7fNZaFWoO/5yZ8uX/7d1JY6G6fQ1xvQ1m1Y6YgHy', '测试用户', 'test@example.com')
ON DUPLICATE KEY UPDATE username=username;

SET FOREIGN_KEY_CHECKS = 1;
