SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- =========================================================
-- clean_gushiwen.sql
-- 适用环境：MySQL 8.0+
--
-- 前置条件：
-- 1. 已经把 gushiwen.sql 导入到 gushiwen_raw 数据库
-- 2. gushiwen_raw 数据库中存在表：shiwen
--
-- 执行方式示例：
-- mysql -u root -p < clean_gushiwen.sql
-- =========================================================

CREATE DATABASE IF NOT EXISTS `poetry_clean`
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;

USE `poetry_clean`;

DROP VIEW IF EXISTS `v_poetry_work_overview`;
DROP TABLE IF EXISTS `work_sentence`;
DROP TABLE IF EXISTS `poetry_work`;
DROP TABLE IF EXISTS `author`;
DROP TABLE IF EXISTS `stg_shiwen_clean`;

DROP FUNCTION IF EXISTS `fn_clean_html`;
DROP FUNCTION IF EXISTS `fn_clean_short_text`;

DELIMITER $$

CREATE FUNCTION `fn_clean_html`(src LONGTEXT)
RETURNS LONGTEXT
DETERMINISTIC
BEGIN
    DECLARE s LONGTEXT;

    IF src IS NULL THEN
        RETURN NULL;
    END IF;

    SET s = src;
    SET s = REPLACE(s, CHAR(13), '');
    SET s = REPLACE(s, '<br/>', '\n');
    SET s = REPLACE(s, '<br />', '\n');
    SET s = REPLACE(s, '<br>', '\n');
    SET s = REPLACE(s, '</p>', '\n');
    SET s = REPLACE(s, '<p>', '');
    SET s = REPLACE(s, '&nbsp;', ' ');
    SET s = REPLACE(s, '&quot;', '"');
    SET s = REPLACE(s, '&ldquo;', '“');
    SET s = REPLACE(s, '&rdquo;', '”');
    SET s = REPLACE(s, '&lsquo;', '‘');
    SET s = REPLACE(s, '&rsquo;', '’');
    SET s = REPLACE(s, '&mdash;', '—');
    SET s = REPLACE(s, '&hellip;', '…');
    SET s = REPLACE(s, '&amp;', '&');
    SET s = REGEXP_REPLACE(s, '<[^>]*>', '');
    SET s = REGEXP_REPLACE(s, '[ ]{2,}', ' ');
    SET s = REGEXP_REPLACE(s, '\n{2,}', '\n');
    RETURN TRIM(s);
END$$

CREATE FUNCTION `fn_clean_short_text`(src TEXT)
RETURNS TEXT
DETERMINISTIC
BEGIN
    DECLARE s TEXT;

    IF src IS NULL THEN
        RETURN NULL;
    END IF;

    SET s = fn_clean_html(src);
    SET s = REPLACE(s, '　', ' ');
    SET s = REGEXP_REPLACE(s, '[ ]{2,}', ' ');
    RETURN TRIM(s);
END$$

DELIMITER ;

CREATE TABLE `stg_shiwen_clean` (
    `raw_id` INT UNSIGNED NOT NULL,
    `href` VARCHAR(128) DEFAULT NULL,
    `title_raw` VARCHAR(1024) DEFAULT NULL,
    `author_raw` VARCHAR(1024) DEFAULT NULL,
    `dynasty_raw` VARCHAR(1024) DEFAULT NULL,
    `content_raw` MEDIUMTEXT,
    `sons_raw` LONGTEXT,
    `links_raw` MEDIUMTEXT,
    `title` VARCHAR(255) DEFAULT NULL,
    `title_search` VARCHAR(255) DEFAULT NULL,
    `author_name` VARCHAR(255) DEFAULT NULL,
    `author_search` VARCHAR(255) DEFAULT NULL,
    `dynasty_name` VARCHAR(64) DEFAULT NULL,
    `content_text` MEDIUMTEXT,
    `translation_raw` MEDIUMTEXT,
    `annotation_raw` MEDIUMTEXT,
    `appreciation_raw` MEDIUMTEXT,
    `translation_text` MEDIUMTEXT,
    `annotation_text` MEDIUMTEXT,
    `appreciation_text` MEDIUMTEXT,
    PRIMARY KEY (`raw_id`),
    UNIQUE KEY `uk_stg_shiwen_href` (`href`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='gushiwen 原始数据清洗中间表';

INSERT INTO `stg_shiwen_clean` (
    `raw_id`,
    `href`,
    `title_raw`,
    `author_raw`,
    `dynasty_raw`,
    `content_raw`,
    `sons_raw`,
    `links_raw`
)
SELECT
    `id`,
    `href`,
    `title`,
    `author`,
    `dynasty`,
    `content`,
    `sons`,
    `links`
FROM `gushiwen_raw`.`shiwen`;

UPDATE `stg_shiwen_clean`
SET
    `title` = fn_clean_short_text(`title_raw`),
    `author_name` = fn_clean_short_text(`author_raw`),
    `dynasty_name` = fn_clean_short_text(`dynasty_raw`),
    `content_text` = fn_clean_html(`content_raw`);

UPDATE `stg_shiwen_clean`
SET
    `title_search` = REPLACE(REPLACE(REPLACE(IFNULL(`title`, ''), ' ', ''), '　', ''), '·', ''),
    `author_search` = REPLACE(REPLACE(REPLACE(IFNULL(`author_name`, ''), ' ', ''), '　', ''), '·', '');

UPDATE `stg_shiwen_clean`
SET `dynasty_name` = CASE `dynasty_name`
    WHEN '先秦' THEN '先秦'
    WHEN '秦朝' THEN '秦代'
    WHEN '汉朝' THEN '汉代'
    WHEN '魏晋' THEN '魏晋'
    WHEN '南北朝' THEN '南北朝'
    WHEN '隋朝' THEN '隋代'
    WHEN '唐朝' THEN '唐代'
    WHEN '宋朝' THEN '宋代'
    WHEN '元朝' THEN '元代'
    WHEN '明朝' THEN '明代'
    WHEN '清朝' THEN '清代'
    ELSE `dynasty_name`
END;

UPDATE `stg_shiwen_clean`
SET
    `translation_raw` = IF(
        JSON_VALID(`sons_raw`),
        JSON_UNQUOTE(
            COALESCE(
                JSON_EXTRACT(`sons_raw`, '$."译文及注释".content'),
                JSON_EXTRACT(`sons_raw`, '$."译文及注释二".content'),
                JSON_EXTRACT(`sons_raw`, '$."译文及注释三".content'),
                JSON_EXTRACT(`sons_raw`, '$."译文".content'),
                JSON_EXTRACT(`sons_raw`, '$."翻译".content'),
                JSON_EXTRACT(`sons_raw`, '$."韵译".content'),
                JSON_EXTRACT(`sons_raw`, '$."参考翻译".content')
            )
        ),
        NULL
    ),
    `annotation_raw` = IF(
        JSON_VALID(`sons_raw`),
        JSON_UNQUOTE(
            COALESCE(
                JSON_EXTRACT(`sons_raw`, '$."注释".content'),
                JSON_EXTRACT(`sons_raw`, '$."注释二".content'),
                JSON_EXTRACT(`sons_raw`, '$."参考注释".content')
            )
        ),
        NULL
    ),
    `appreciation_raw` = IF(
        JSON_VALID(`sons_raw`),
        JSON_UNQUOTE(
            COALESCE(
                JSON_EXTRACT(`sons_raw`, '$."赏析".content'),
                JSON_EXTRACT(`sons_raw`, '$."赏析二".content'),
                JSON_EXTRACT(`sons_raw`, '$."鉴赏".content'),
                JSON_EXTRACT(`sons_raw`, '$."评析".content'),
                JSON_EXTRACT(`sons_raw`, '$."简析".content'),
                JSON_EXTRACT(`sons_raw`, '$."创作背景".content'),
                JSON_EXTRACT(`sons_raw`, '$."写作背景".content')
            )
        ),
        NULL
    );

UPDATE `stg_shiwen_clean`
SET
    `translation_text` = fn_clean_html(`translation_raw`),
    `annotation_text` = fn_clean_html(`annotation_raw`),
    `appreciation_text` = fn_clean_html(`appreciation_raw`);

UPDATE `stg_shiwen_clean`
SET
    `translation_text` = TRIM(SUBSTRING_INDEX(IFNULL(`translation_text`, ''), '参考资料：', 1)),
    `annotation_text` = TRIM(SUBSTRING_INDEX(IFNULL(`annotation_text`, ''), '参考资料：', 1)),
    `appreciation_text` = TRIM(SUBSTRING_INDEX(IFNULL(`appreciation_text`, ''), '参考资料：', 1));

UPDATE `stg_shiwen_clean`
SET
    `translation_text` = TRIM(REGEXP_REPLACE(IFNULL(`translation_text`, ''), '^(译文|翻译|韵译)\s*', '')),
    `annotation_text` = TRIM(REGEXP_REPLACE(IFNULL(`annotation_text`, ''), '^注释\s*', '')),
    `appreciation_text` = TRIM(REGEXP_REPLACE(IFNULL(`appreciation_text`, ''), '^(赏析|鉴赏|评析|简析|创作背景|写作背景)\s*', ''));

UPDATE `stg_shiwen_clean`
SET
    `annotation_text` = CASE
        WHEN (`annotation_text` IS NULL OR `annotation_text` = '')
             AND `translation_text` LIKE '%注释%'
            THEN TRIM(REGEXP_REPLACE(SUBSTRING_INDEX(`translation_text`, '注释', -1), '^注释\s*', ''))
        ELSE `annotation_text`
    END,
    `translation_text` = CASE
        WHEN `translation_text` LIKE '%注释%'
            THEN TRIM(SUBSTRING_INDEX(`translation_text`, '注释', 1))
        ELSE `translation_text`
    END;

UPDATE `stg_shiwen_clean`
SET
    `translation_text` = NULLIF(TRIM(`translation_text`), ''),
    `annotation_text` = NULLIF(TRIM(`annotation_text`), ''),
    `appreciation_text` = NULLIF(TRIM(`appreciation_text`), ''),
    `content_text` = NULLIF(TRIM(`content_text`), '');

CREATE TABLE `author` (
    `author_id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `canonical_name` VARCHAR(255) NOT NULL,
    `author_search` VARCHAR(255) DEFAULT NULL,
    `dynasty_name` VARCHAR(64) DEFAULT NULL,
    `source_code` VARCHAR(32) NOT NULL DEFAULT 'yht050511',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`author_id`),
    UNIQUE KEY `uk_author_name_dynasty` (`canonical_name`, `dynasty_name`),
    KEY `idx_author_search` (`author_search`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='作者表';

INSERT INTO `author` (
    `canonical_name`,
    `author_search`,
    `dynasty_name`
)
SELECT DISTINCT
    `author_name`,
    `author_search`,
    `dynasty_name`
FROM `stg_shiwen_clean`
WHERE `author_name` IS NOT NULL
  AND `author_name` <> '';

CREATE TABLE `poetry_work` (
    `work_id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `raw_id` INT UNSIGNED NOT NULL,
    `source_code` VARCHAR(32) NOT NULL DEFAULT 'yht050511',
    `href` VARCHAR(128) DEFAULT NULL,
    `title` VARCHAR(255) NOT NULL,
    `title_search` VARCHAR(255) DEFAULT NULL,
    `author_id` BIGINT UNSIGNED DEFAULT NULL,
    `author_name_cache` VARCHAR(255) DEFAULT NULL,
    `author_search` VARCHAR(255) DEFAULT NULL,
    `dynasty_name` VARCHAR(64) DEFAULT NULL,
    `content_text` MEDIUMTEXT,
    `translation_text` MEDIUMTEXT,
    `annotation_text` MEDIUMTEXT,
    `appreciation_text` MEDIUMTEXT,
    `sons_raw` LONGTEXT,
    `links_raw` MEDIUMTEXT,
    `content_hash` CHAR(64) DEFAULT NULL,
    `line_count` INT UNSIGNED NOT NULL DEFAULT 0,
    `char_count` INT UNSIGNED NOT NULL DEFAULT 0,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`work_id`),
    UNIQUE KEY `uk_poetry_work_raw_id` (`raw_id`),
    UNIQUE KEY `uk_poetry_work_href` (`href`),
    KEY `idx_poetry_work_title_search` (`title_search`),
    KEY `idx_poetry_work_author_id` (`author_id`),
    KEY `idx_poetry_work_author_search` (`author_search`),
    KEY `idx_poetry_work_dynasty_name` (`dynasty_name`),
    KEY `idx_poetry_work_content_hash` (`content_hash`),
    CONSTRAINT `fk_poetry_work_author`
        FOREIGN KEY (`author_id`) REFERENCES `author` (`author_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='清洗后的作品表';

INSERT INTO `poetry_work` (
    `raw_id`,
    `href`,
    `title`,
    `title_search`,
    `author_id`,
    `author_name_cache`,
    `author_search`,
    `dynasty_name`,
    `content_text`,
    `translation_text`,
    `annotation_text`,
    `appreciation_text`,
    `sons_raw`,
    `links_raw`,
    `content_hash`,
    `line_count`,
    `char_count`
)
SELECT
    s.`raw_id`,
    s.`href`,
    s.`title`,
    s.`title_search`,
    a.`author_id`,
    s.`author_name`,
    s.`author_search`,
    s.`dynasty_name`,
    s.`content_text`,
    s.`translation_text`,
    s.`annotation_text`,
    s.`appreciation_text`,
    s.`sons_raw`,
    s.`links_raw`,
    SHA2(IFNULL(s.`content_text`, ''), 256),
    CASE
        WHEN s.`content_text` IS NULL OR s.`content_text` = '' THEN 0
        ELSE 1 + CHAR_LENGTH(s.`content_text`) - CHAR_LENGTH(REPLACE(s.`content_text`, '\n', ''))
    END AS `line_count`,
    CHAR_LENGTH(
        REPLACE(
            REPLACE(
                REPLACE(IFNULL(s.`content_text`, ''), '\n', ''),
                ' ',
                ''
            ),
            '　',
            ''
        )
    ) AS `char_count`
FROM `stg_shiwen_clean` s
LEFT JOIN `author` a
    ON a.`canonical_name` = s.`author_name`
   AND (
        (a.`dynasty_name` = s.`dynasty_name`)
        OR (a.`dynasty_name` IS NULL AND s.`dynasty_name` IS NULL)
   );

CREATE TABLE `work_sentence` (
    `sentence_id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `work_id` BIGINT UNSIGNED NOT NULL,
    `sentence_no` INT UNSIGNED NOT NULL,
    `sentence_text` TEXT NOT NULL,
    `char_count` INT UNSIGNED NOT NULL DEFAULT 0,
    PRIMARY KEY (`sentence_id`),
    UNIQUE KEY `uk_work_sentence` (`work_id`, `sentence_no`),
    KEY `idx_work_sentence_work_id` (`work_id`),
    CONSTRAINT `fk_work_sentence_work`
        FOREIGN KEY (`work_id`) REFERENCES `poetry_work` (`work_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='句子表，供填空/飞花令/接龙使用';

INSERT INTO `work_sentence` (
    `work_id`,
    `sentence_no`,
    `sentence_text`,
    `char_count`
)
WITH RECURSIVE sentence_split AS (
    SELECT
        seed.`work_id`,
        1 AS `sentence_no`,
        TRIM(SUBSTRING_INDEX(seed.`sentence_source`, '|', 1)) AS `sentence_text`,
        CASE
            WHEN INSTR(seed.`sentence_source`, '|') > 0
                THEN SUBSTRING(seed.`sentence_source`, INSTR(seed.`sentence_source`, '|') + 1)
            ELSE ''
        END AS `rest_text`
    FROM (
        SELECT
            `work_id`,
            TRIM(BOTH '|' FROM
                REGEXP_REPLACE(
                REPLACE(
                    REPLACE(
                        REPLACE(
                            REPLACE(
                                REPLACE(
                                    REPLACE(
                                        REPLACE(
                                            REPLACE(IFNULL(`content_text`, ''), '\n', '|'),
                                            '，',
                                            '，|'
                                        ),
                                        '。', '。|'
                                    ),
                                    '！', '！|'
                                ),
                                    '？', '？|'
                                ),
                                '；', '；|'
                            ),
                            ';', ';|'
                        ),
                        '!', '!|'
                    ),
                    '\\|{2,}',
                    '|'
                )
            ) AS `sentence_source`
        FROM `poetry_work`
        WHERE `content_text` IS NOT NULL
          AND `content_text` <> ''
    ) seed

    UNION ALL

    SELECT
        `work_id`,
        `sentence_no` + 1,
        TRIM(SUBSTRING_INDEX(`rest_text`, '|', 1)) AS `sentence_text`,
        CASE
            WHEN INSTR(`rest_text`, '|') > 0
                THEN SUBSTRING(`rest_text`, INSTR(`rest_text`, '|') + 1)
            ELSE ''
        END AS `rest_text`
    FROM `sentence_split`
    WHERE `rest_text` IS NOT NULL
      AND `rest_text` <> ''
      AND `sentence_no` < 80
)
SELECT
    `work_id`,
    `sentence_no`,
    `sentence_text`,
    CHAR_LENGTH(
        REPLACE(
            REPLACE(
                REPLACE(`sentence_text`, '\n', ''),
                ' ',
                ''
            ),
            '　',
            ''
        )
    ) AS `char_count`
FROM `sentence_split`
WHERE `sentence_text` IS NOT NULL
  AND `sentence_text` <> '';

CREATE OR REPLACE VIEW `v_poetry_work_overview` AS
SELECT
    w.`work_id`,
    w.`raw_id`,
    w.`title`,
    w.`author_name_cache` AS `author_name`,
    w.`dynasty_name`,
    w.`line_count`,
    w.`char_count`,
    w.`href`,
    LEFT(w.`content_text`, 120) AS `content_preview`,
    CASE WHEN w.`translation_text` IS NOT NULL AND w.`translation_text` <> '' THEN 1 ELSE 0 END AS `has_translation`,
    CASE WHEN w.`annotation_text` IS NOT NULL AND w.`annotation_text` <> '' THEN 1 ELSE 0 END AS `has_annotation`,
    CASE WHEN w.`appreciation_text` IS NOT NULL AND w.`appreciation_text` <> '' THEN 1 ELSE 0 END AS `has_appreciation`
FROM `poetry_work` w;

SET FOREIGN_KEY_CHECKS = 1;

-- =========================================================
-- 执行完成后可用以下 SQL 进行检查：
--
-- USE poetry_clean;
-- SHOW TABLES;
-- SELECT COUNT(*) AS author_count FROM author;
-- SELECT COUNT(*) AS work_count FROM poetry_work;
-- SELECT COUNT(*) AS sentence_count FROM work_sentence;
-- SELECT * FROM v_poetry_work_overview LIMIT 10;
-- SELECT title, author_name_cache, dynasty_name FROM poetry_work LIMIT 10;
-- =========================================================
