SET NAMES utf8mb4;

USE `poetry_clean`;

SET @has_work_tag_idx := (
    SELECT COUNT(*)
    FROM information_schema.statistics
    WHERE table_schema = 'poetry_clean'
      AND table_name = 'work_tag'
      AND index_name = 'idx_work_tag_tag_work'
);

SET @sql_work_tag_idx := IF(
    @has_work_tag_idx = 0,
    'ALTER TABLE work_tag ADD INDEX idx_work_tag_tag_work (tag_id, work_id)',
    'SELECT ''idx_work_tag_tag_work already exists'''
);

PREPARE stmt_work_tag_idx FROM @sql_work_tag_idx;
EXECUTE stmt_work_tag_idx;
DEALLOCATE PREPARE stmt_work_tag_idx;

SET @has_poetry_created_idx := (
    SELECT COUNT(*)
    FROM information_schema.statistics
    WHERE table_schema = 'poetry_clean'
      AND table_name = 'poetry_work'
      AND index_name = 'idx_poetry_work_created_at'
);

SET @sql_poetry_created_idx := IF(
    @has_poetry_created_idx = 0,
    'ALTER TABLE poetry_work ADD INDEX idx_poetry_work_created_at (created_at)',
    'SELECT ''idx_poetry_work_created_at already exists'''
);

PREPARE stmt_poetry_created_idx FROM @sql_poetry_created_idx;
EXECUTE stmt_poetry_created_idx;
DEALLOCATE PREPARE stmt_poetry_created_idx;
