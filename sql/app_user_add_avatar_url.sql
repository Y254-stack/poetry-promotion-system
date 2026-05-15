-- 若 app_user 表缺少 avatar_url（例如由精简 auth 脚本初始化），执行本脚本补齐。
-- MySQL 5.7+ / 8.0+

SET @col_exists := (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'app_user'
      AND COLUMN_NAME = 'avatar_url'
);

SET @sql := IF(
    @col_exists = 0,
    'ALTER TABLE app_user ADD COLUMN avatar_url VARCHAR(512) DEFAULT NULL COMMENT ''头像 URL 或相对路径''',
    'SELECT ''avatar_url already exists'' AS message'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
