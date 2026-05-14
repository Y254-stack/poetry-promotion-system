-- 登录失败计数与临时锁定（已有库执行一次即可；全新库请使用已更新的 poetry_full_init.sql）
ALTER TABLE `app_user`
    ADD COLUMN `login_locked_until` DATETIME NULL DEFAULT NULL AFTER `status`,
    ADD COLUMN `failed_login_count` INT UNSIGNED NOT NULL DEFAULT 0 AFTER `login_locked_until`,
    ADD COLUMN `failed_login_window_start` DATETIME NULL DEFAULT NULL AFTER `failed_login_count`;
