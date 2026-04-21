SET NAMES utf8mb4;

-- =========================================================
-- poetry_full_init.sql
-- 一体化数据库初始化脚本
-- 执行顺序：
-- 1. clean_gushiwen.sql
-- 2. enrich_recommendation.sql
-- 3. poetry_team_schema.sql
--
-- 前置条件：
-- 1. 已经把 gushiwen.sql 导入到 gushiwen_raw.shiwen
-- 2. 使用 MySQL 8.0+
-- =========================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- =========================================================
-- clean_gushiwen.sql
-- 閫傜敤鐜锛歁ySQL 8.0+
--
-- 鍓嶇疆鏉′欢锛?-- 1. 宸茬粡鎶?gushiwen.sql 瀵煎叆鍒?gushiwen_raw 鏁版嵁搴?-- 2. gushiwen_raw 鏁版嵁搴撲腑瀛樺湪琛細shiwen
--
-- 鎵ц鏂瑰紡绀轰緥锛?-- mysql -u root -p < clean_gushiwen.sql
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
    SET s = REPLACE(s, '&ldquo;', '鈥?);
    SET s = REPLACE(s, '&rdquo;', '鈥?);
    SET s = REPLACE(s, '&lsquo;', '鈥?);
    SET s = REPLACE(s, '&rsquo;', '鈥?);
    SET s = REPLACE(s, '&mdash;', '鈥?);
    SET s = REPLACE(s, '&hellip;', '鈥?);
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
    SET s = REPLACE(s, '銆€', ' ');
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='gushiwen 鍘熷鏁版嵁娓呮礂涓棿琛?;

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
    `title_search` = REPLACE(REPLACE(REPLACE(IFNULL(`title`, ''), ' ', ''), '銆€', ''), '路', ''),
    `author_search` = REPLACE(REPLACE(REPLACE(IFNULL(`author_name`, ''), ' ', ''), '銆€', ''), '路', '');

UPDATE `stg_shiwen_clean`
SET `dynasty_name` = CASE `dynasty_name`
    WHEN '鍏堢Е' THEN '鍏堢Е'
    WHEN '绉︽湞' THEN '绉︿唬'
    WHEN '姹夋湞' THEN '姹変唬'
    WHEN '榄忔檵' THEN '榄忔檵'
    WHEN '鍗楀寳鏈? THEN '鍗楀寳鏈?
    WHEN '闅嬫湞' THEN '闅嬩唬'
    WHEN '鍞愭湞' THEN '鍞愪唬'
    WHEN '瀹嬫湞' THEN '瀹嬩唬'
    WHEN '鍏冩湞' THEN '鍏冧唬'
    WHEN '鏄庢湞' THEN '鏄庝唬'
    WHEN '娓呮湞' THEN '娓呬唬'
    ELSE `dynasty_name`
END;

UPDATE `stg_shiwen_clean`
SET
    `translation_raw` = IF(
        JSON_VALID(`sons_raw`),
        JSON_UNQUOTE(
            COALESCE(
                JSON_EXTRACT(`sons_raw`, '$."璇戞枃鍙婃敞閲?.content'),
                JSON_EXTRACT(`sons_raw`, '$."璇戞枃鍙婃敞閲婁簩".content'),
                JSON_EXTRACT(`sons_raw`, '$."璇戞枃鍙婃敞閲婁笁".content'),
                JSON_EXTRACT(`sons_raw`, '$."璇戞枃".content'),
                JSON_EXTRACT(`sons_raw`, '$."缈昏瘧".content'),
                JSON_EXTRACT(`sons_raw`, '$."闊佃瘧".content'),
                JSON_EXTRACT(`sons_raw`, '$."鍙傝€冪炕璇?.content')
            )
        ),
        NULL
    ),
    `annotation_raw` = IF(
        JSON_VALID(`sons_raw`),
        JSON_UNQUOTE(
            COALESCE(
                JSON_EXTRACT(`sons_raw`, '$."娉ㄩ噴".content'),
                JSON_EXTRACT(`sons_raw`, '$."娉ㄩ噴浜?.content'),
                JSON_EXTRACT(`sons_raw`, '$."鍙傝€冩敞閲?.content')
            )
        ),
        NULL
    ),
    `appreciation_raw` = IF(
        JSON_VALID(`sons_raw`),
        JSON_UNQUOTE(
            COALESCE(
                JSON_EXTRACT(`sons_raw`, '$."璧忔瀽".content'),
                JSON_EXTRACT(`sons_raw`, '$."璧忔瀽浜?.content'),
                JSON_EXTRACT(`sons_raw`, '$."閴磋祻".content'),
                JSON_EXTRACT(`sons_raw`, '$."璇勬瀽".content'),
                JSON_EXTRACT(`sons_raw`, '$."绠€鏋?.content'),
                JSON_EXTRACT(`sons_raw`, '$."鍒涗綔鑳屾櫙".content'),
                JSON_EXTRACT(`sons_raw`, '$."鍐欎綔鑳屾櫙".content')
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
    `translation_text` = TRIM(SUBSTRING_INDEX(IFNULL(`translation_text`, ''), '鍙傝€冭祫鏂欙細', 1)),
    `annotation_text` = TRIM(SUBSTRING_INDEX(IFNULL(`annotation_text`, ''), '鍙傝€冭祫鏂欙細', 1)),
    `appreciation_text` = TRIM(SUBSTRING_INDEX(IFNULL(`appreciation_text`, ''), '鍙傝€冭祫鏂欙細', 1));

UPDATE `stg_shiwen_clean`
SET
    `translation_text` = TRIM(REGEXP_REPLACE(IFNULL(`translation_text`, ''), '^(璇戞枃|缈昏瘧|闊佃瘧)\s*', '')),
    `annotation_text` = TRIM(REGEXP_REPLACE(IFNULL(`annotation_text`, ''), '^娉ㄩ噴\s*', '')),
    `appreciation_text` = TRIM(REGEXP_REPLACE(IFNULL(`appreciation_text`, ''), '^(璧忔瀽|閴磋祻|璇勬瀽|绠€鏋恷鍒涗綔鑳屾櫙|鍐欎綔鑳屾櫙)\s*', ''));

UPDATE `stg_shiwen_clean`
SET
    `annotation_text` = CASE
        WHEN (`annotation_text` IS NULL OR `annotation_text` = '')
             AND `translation_text` LIKE '%娉ㄩ噴%'
            THEN TRIM(REGEXP_REPLACE(SUBSTRING_INDEX(`translation_text`, '娉ㄩ噴', -1), '^娉ㄩ噴\s*', ''))
        ELSE `annotation_text`
    END,
    `translation_text` = CASE
        WHEN `translation_text` LIKE '%娉ㄩ噴%'
            THEN TRIM(SUBSTRING_INDEX(`translation_text`, '娉ㄩ噴', 1))
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='浣滆€呰〃';

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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='娓呮礂鍚庣殑浣滃搧琛?;

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
            '銆€',
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='鍙ュ瓙琛紝渚涘～绌?椋炶姳浠?鎺ラ緳浣跨敤';

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
                                            '锛?,
                                            '锛寍'
                                        ),
                                        '銆?, '銆倈'
                                    ),
                                    '锛?, '锛亅'
                                ),
                                    '锛?, '锛焲'
                                ),
                                '锛?, '锛泑'
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
            '銆€',
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
-- 鎵ц瀹屾垚鍚庡彲鐢ㄤ互涓?SQL 杩涜妫€鏌ワ細
--
-- USE poetry_clean;
-- SHOW TABLES;
-- SELECT COUNT(*) AS author_count FROM author;
-- SELECT COUNT(*) AS work_count FROM poetry_work;
-- SELECT COUNT(*) AS sentence_count FROM work_sentence;
-- SELECT * FROM v_poetry_work_overview LIMIT 10;
-- SELECT title, author_name_cache, dynasty_name FROM poetry_work LIMIT 10;
-- =========================================================

-- =========================================================
-- recommendation enrichment
-- =========================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

USE `poetry_clean`;

DROP VIEW IF EXISTS `v_daily_recommend_today`;
DROP TABLE IF EXISTS `daily_recommend_item`;
DROP TABLE IF EXISTS `daily_recommendation`;
DROP TABLE IF EXISTS `work_relation`;

CREATE TABLE `work_relation` (
    `relation_id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `work_id` BIGINT UNSIGNED NOT NULL,
    `related_work_id` BIGINT UNSIGNED NOT NULL,
    `relation_type` VARCHAR(32) NOT NULL,
    `score` DECIMAL(8,2) NOT NULL DEFAULT 0.00,
    `source_note` VARCHAR(255) DEFAULT NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`relation_id`),
    UNIQUE KEY `uk_work_relation` (`work_id`, `related_work_id`, `relation_type`),
    KEY `idx_work_relation_work` (`work_id`),
    KEY `idx_work_relation_related` (`related_work_id`),
    KEY `idx_work_relation_type` (`relation_type`),
    CONSTRAINT `fk_work_relation_work`
        FOREIGN KEY (`work_id`) REFERENCES `poetry_work` (`work_id`) ON DELETE CASCADE,
    CONSTRAINT `fk_work_relation_related_work`
        FOREIGN KEY (`related_work_id`) REFERENCES `poetry_work` (`work_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='浣滃搧鐩稿叧鎺ㄨ崘琛?;

CREATE TABLE `daily_recommendation` (
    `recommendation_id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `recommend_date` DATE NOT NULL,
    `theme_name` VARCHAR(64) NOT NULL,
    `intro_text` VARCHAR(255) DEFAULT NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`recommendation_id`),
    UNIQUE KEY `uk_daily_recommend_date` (`recommend_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='姣忔棩鎺ㄨ崘涓昏〃';

CREATE TABLE `daily_recommend_item` (
    `item_id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `recommendation_id` BIGINT UNSIGNED NOT NULL,
    `sort_no` INT UNSIGNED NOT NULL,
    `work_id` BIGINT UNSIGNED NOT NULL,
    `reason_type` VARCHAR(32) DEFAULT NULL,
    `reason_text` VARCHAR(255) DEFAULT NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`item_id`),
    UNIQUE KEY `uk_daily_recommend_item_sort` (`recommendation_id`, `sort_no`),
    UNIQUE KEY `uk_daily_recommend_item_work` (`recommendation_id`, `work_id`),
    KEY `idx_daily_recommend_item_work` (`work_id`),
    CONSTRAINT `fk_daily_recommend_item_recommendation`
        FOREIGN KEY (`recommendation_id`) REFERENCES `daily_recommendation` (`recommendation_id`) ON DELETE CASCADE,
    CONSTRAINT `fk_daily_recommend_item_work`
        FOREIGN KEY (`work_id`) REFERENCES `poetry_work` (`work_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='姣忔棩鎺ㄨ崘鏄庣粏琛?;

DROP TEMPORARY TABLE IF EXISTS `tmp_work_quality`;
CREATE TEMPORARY TABLE `tmp_work_quality` AS
SELECT
    w.`work_id`,
    w.`author_id`,
    w.`dynasty_name`,
    w.`title`,
    w.`author_name_cache`,
    CASE WHEN w.`translation_text` IS NOT NULL AND w.`translation_text` <> '' THEN 1 ELSE 0 END AS `has_translation`,
    CASE WHEN w.`annotation_text` IS NOT NULL AND w.`annotation_text` <> '' THEN 1 ELSE 0 END AS `has_annotation`,
    CASE WHEN w.`appreciation_text` IS NOT NULL AND w.`appreciation_text` <> '' THEN 1 ELSE 0 END AS `has_appreciation`,
    (
        CASE WHEN w.`translation_text` IS NOT NULL AND w.`translation_text` <> '' THEN 30 ELSE 0 END +
        CASE WHEN w.`annotation_text` IS NOT NULL AND w.`annotation_text` <> '' THEN 25 ELSE 0 END +
        CASE WHEN w.`appreciation_text` IS NOT NULL AND w.`appreciation_text` <> '' THEN 20 ELSE 0 END +
        CASE WHEN w.`char_count` BETWEEN 12 AND 200 THEN 10 ELSE 0 END +
        CASE WHEN w.`dynasty_name` IN ('鍞愪唬', '瀹嬩唬') THEN 8 ELSE 0 END +
        CASE WHEN w.`author_name_cache` IN ('鏉庣櫧', '鏉滅敨', '鐧藉眳鏄?, '鑻忚郊', '杈涘純鐤?, '鏉庢竻鐓?, '鐜嬬淮', '鏉庡晢闅?, '鏉滅墽', '闄舵笂鏄?)
             THEN 12 ELSE 0 END
    ) AS `quality_score`
FROM `poetry_work` w;

-- ---------------------------------------------------------
-- 1. 鏄惧紡鐩稿叧鎺ㄨ崘锛氱洿鎺ヤ娇鐢ㄥ師濮?links_raw 涓殑 href
-- ---------------------------------------------------------

DROP TEMPORARY TABLE IF EXISTS `tmp_link_source`;
CREATE TEMPORARY TABLE `tmp_link_source` AS
SELECT
    w.`work_id`,
    TRIM(BOTH ',' FROM
        REPLACE(
            REPLACE(
                REPLACE(
                    REPLACE(IFNULL(w.`links_raw`, ''), '[', ''),
                    ']',
                    ''
                ),
                '''',
                ''
            ),
            ' ',
            ''
        )
    ) AS `href_csv`
FROM `poetry_work` w
WHERE w.`links_raw` IS NOT NULL
  AND w.`links_raw` <> '';

INSERT IGNORE INTO `work_relation` (
    `work_id`,
    `related_work_id`,
    `relation_type`,
    `score`,
    `source_note`
)
WITH RECURSIVE `link_split` AS (
    SELECT
        src.`work_id`,
        1 AS `depth_no`,
        TRIM(SUBSTRING_INDEX(src.`href_csv`, ',', 1)) AS `related_href`,
        CASE
            WHEN INSTR(src.`href_csv`, ',') > 0
                THEN SUBSTRING(src.`href_csv`, INSTR(src.`href_csv`, ',') + 1)
            ELSE ''
        END AS `rest_csv`
    FROM `tmp_link_source` src

    UNION ALL

    SELECT
        `work_id`,
        `depth_no` + 1,
        TRIM(SUBSTRING_INDEX(`rest_csv`, ',', 1)) AS `related_href`,
        CASE
            WHEN INSTR(`rest_csv`, ',') > 0
                THEN SUBSTRING(`rest_csv`, INSTR(`rest_csv`, ',') + 1)
            ELSE ''
        END AS `rest_csv`
    FROM `link_split`
    WHERE `rest_csv` <> ''
      AND `depth_no` < 20
)
SELECT
    ls.`work_id`,
    dst.`work_id`,
    'explicit_link' AS `relation_type`,
    100.00 AS `score`,
    'links_raw 鏄惧紡鍏宠仈' AS `source_note`
FROM `link_split` ls
JOIN `poetry_work` dst
    ON dst.`href` = ls.`related_href`
WHERE ls.`related_href` <> ''
  AND ls.`work_id` <> dst.`work_id`;

-- ---------------------------------------------------------
-- 2. 鍚屼綔鑰呯浉鍏虫帹鑽愶細鎸変綔鑰呭唴璐ㄩ噺鍒嗘帓搴忥紝鍙栫浉閭讳綔鍝?-- ---------------------------------------------------------

DROP TEMPORARY TABLE IF EXISTS `tmp_author_rank`;
CREATE TEMPORARY TABLE `tmp_author_rank` AS
SELECT
    q.`work_id`,
    q.`author_id`,
    q.`quality_score`,
    ROW_NUMBER() OVER (
        PARTITION BY q.`author_id`
        ORDER BY q.`quality_score` DESC, q.`work_id`
    ) AS `rn`
FROM `tmp_work_quality` q
WHERE q.`author_id` IS NOT NULL;

DROP TEMPORARY TABLE IF EXISTS `tmp_author_rank_b`;
CREATE TEMPORARY TABLE `tmp_author_rank_b` AS
SELECT * FROM `tmp_author_rank`;

INSERT IGNORE INTO `work_relation` (
    `work_id`,
    `related_work_id`,
    `relation_type`,
    `score`,
    `source_note`
)
SELECT
    a.`work_id`,
    b.`work_id`,
    'same_author' AS `relation_type`,
    85 - (ABS(CAST(a.`rn` AS SIGNED) - CAST(b.`rn` AS SIGNED)) * 5) AS `score`,
    '鍚屼綔鑰呯浉閭讳綔鍝? AS `source_note`
FROM `tmp_author_rank` a
JOIN `tmp_author_rank_b` b
    ON a.`author_id` = b.`author_id`
   AND a.`work_id` <> b.`work_id`
   AND ABS(CAST(a.`rn` AS SIGNED) - CAST(b.`rn` AS SIGNED)) BETWEEN 1 AND 3;

-- ---------------------------------------------------------
-- 3. 鍚屾湞浠ｇ浉鍏虫帹鑽愶細鎸夋湞浠ｅ唴璐ㄩ噺鍒嗘帓搴忥紝鍙栫浉閭讳綔鍝?-- ---------------------------------------------------------

DROP TEMPORARY TABLE IF EXISTS `tmp_dynasty_rank`;
CREATE TEMPORARY TABLE `tmp_dynasty_rank` AS
SELECT
    q.`work_id`,
    q.`author_id`,
    q.`dynasty_name`,
    q.`quality_score`,
    ROW_NUMBER() OVER (
        PARTITION BY q.`dynasty_name`
        ORDER BY q.`quality_score` DESC, q.`work_id`
    ) AS `rn`
FROM `tmp_work_quality` q
WHERE q.`dynasty_name` IS NOT NULL
  AND q.`dynasty_name` <> '';

DROP TEMPORARY TABLE IF EXISTS `tmp_dynasty_rank_b`;
CREATE TEMPORARY TABLE `tmp_dynasty_rank_b` AS
SELECT * FROM `tmp_dynasty_rank`;

INSERT IGNORE INTO `work_relation` (
    `work_id`,
    `related_work_id`,
    `relation_type`,
    `score`,
    `source_note`
)
SELECT
    a.`work_id`,
    b.`work_id`,
    'same_dynasty' AS `relation_type`,
    62 - (ABS(CAST(a.`rn` AS SIGNED) - CAST(b.`rn` AS SIGNED)) * 4) AS `score`,
    '鍚屾湞浠ｇ浉閭讳綔鍝? AS `source_note`
FROM `tmp_dynasty_rank` a
JOIN `tmp_dynasty_rank_b` b
    ON a.`dynasty_name` = b.`dynasty_name`
   AND a.`work_id` <> b.`work_id`
   AND ABS(CAST(a.`rn` AS SIGNED) - CAST(b.`rn` AS SIGNED)) BETWEEN 1 AND 2
   AND (a.`author_id` IS NULL OR b.`author_id` IS NULL OR a.`author_id` <> b.`author_id`);

-- ---------------------------------------------------------
-- 4. 姣忔棩鎺ㄨ崘涓昏〃锛氱敓鎴愪粠浠婂ぉ寮€濮?30 澶╂帹鑽?-- ---------------------------------------------------------

DROP TEMPORARY TABLE IF EXISTS `tmp_date_series`;
CREATE TEMPORARY TABLE `tmp_date_series` AS
WITH RECURSIVE `date_series` AS (
    SELECT CURDATE() AS `recommend_date`, 0 AS `offset_day`
    UNION ALL
    SELECT DATE_ADD(`recommend_date`, INTERVAL 1 DAY), `offset_day` + 1
    FROM `date_series`
    WHERE `offset_day` < 29
)
SELECT
    `recommend_date`,
    `offset_day`
FROM `date_series`;

INSERT INTO `daily_recommendation` (
    `recommend_date`,
    `theme_name`,
    `intro_text`
)
SELECT
    `recommend_date`,
    CASE MOD(`offset_day`, 7)
        WHEN 0 THEN '鏄ユ棩璇楁剰'
        WHEN 1 THEN '灞辨按娓呴煶'
        WHEN 2 THEN '鏄庢湀鐩告€?
        WHEN 3 THEN '瀹跺浗鎯呮€€'
        WHEN 4 THEN '閫佸埆绂绘剚'
        WHEN 5 THEN '鐢板洯闂查€?
        ELSE '璞斁姘旇薄'
    END AS `theme_name`,
    CASE MOD(`offset_day`, 7)
        WHEN 0 THEN '鍥寸粫鏄ユ剰涓婚鎺ㄨ崘閫傚悎纰庣墖鍖栭槄璇荤殑缁忓吀璇楄瘝銆?
        WHEN 1 THEN '鑱氱劍灞辨按鎰忚薄锛岄€傚悎娴忚涓庡崱鐗囧垱浣溿€?
        WHEN 2 THEN '浠ユ槑鏈堛€佹€濆康鐩稿叧浣滃搧涓轰富锛岄€傚悎澶滈棿闃呰銆?
        WHEN 3 THEN '浼樺厛鎺ㄨ崘瀹跺浗杈瑰涓婚鍚嶇瘒銆?
        WHEN 4 THEN '绮鹃€夐€佸埆涓庣鎰佷富棰樹綔鍝併€?
        WHEN 5 THEN '鎺ㄨ崘鐢板洯涓庡綊闅愭皵璐ㄨ緝寮虹殑浣滃搧銆?
        ELSE '灞曠ず鏇村叿璞斁姘旇川鍜屼紶鎾€х殑缁忓吀绡囩洰銆?
    END AS `intro_text`
FROM `tmp_date_series`;

-- ---------------------------------------------------------
-- 5. 姣忔棩鎺ㄨ崘鍊欓€夋睜锛氬彧浠庤川閲忚緝楂樼殑浣滃搧閲岄€?-- ---------------------------------------------------------

DROP TEMPORARY TABLE IF EXISTS `tmp_daily_candidate`;
CREATE TEMPORARY TABLE `tmp_daily_candidate` AS
SELECT
    w.`work_id`,
    w.`title`,
    w.`author_name_cache`,
    w.`dynasty_name`,
    q.`quality_score`,
    q.`has_translation`,
    q.`has_annotation`,
    q.`has_appreciation`
FROM `poetry_work` w
JOIN `tmp_work_quality` q
    ON q.`work_id` = w.`work_id`
WHERE w.`content_text` IS NOT NULL
  AND w.`content_text` <> ''
  AND w.`char_count` BETWEEN 12 AND 220
  AND q.`quality_score` >= 20;

-- ---------------------------------------------------------
-- 6. 姣忔棩鎺ㄨ崘鏄庣粏锛氭瘡涓€澶╅€?5 鏉?-- ---------------------------------------------------------

INSERT INTO `daily_recommend_item` (
    `recommendation_id`,
    `sort_no`,
    `work_id`,
    `reason_type`,
    `reason_text`
)
SELECT
    ranked.`recommendation_id`,
    ranked.`sort_no`,
    ranked.`work_id`,
    ranked.`reason_type`,
    ranked.`reason_text`
FROM (
    SELECT
        r.`recommendation_id`,
        c.`work_id`,
        ROW_NUMBER() OVER (
            PARTITION BY r.`recommendation_id`
            ORDER BY
                (
                    CASE r.`theme_name`
                        WHEN '鏄ユ棩璇楁剰' THEN
                            (CASE WHEN c.`title` LIKE '%鏄?' THEN 200 ELSE 0 END)
                        WHEN '灞辨按娓呴煶' THEN
                            (CASE WHEN c.`title` LIKE '%灞?' OR c.`title` LIKE '%姘?' THEN 200 ELSE 0 END)
                        WHEN '鏄庢湀鐩告€? THEN
                            (CASE WHEN c.`title` LIKE '%鏈?' THEN 200 ELSE 0 END)
                        WHEN '瀹跺浗鎯呮€€' THEN
                            (CASE WHEN c.`title` LIKE '%鍥?' OR c.`title` LIKE '%濉?' OR c.`title` LIKE '%杈?' THEN 200 ELSE 0 END)
                        WHEN '閫佸埆绂绘剚' THEN
                            (CASE WHEN c.`title` LIKE '%閫?' OR c.`title` LIKE '%鍒?' OR c.`title` LIKE '%鎰?' THEN 200 ELSE 0 END)
                        WHEN '鐢板洯闂查€? THEN
                            (CASE WHEN c.`title` LIKE '%鐢?' OR c.`title` LIKE '%鍥?' OR c.`title` LIKE '%褰?' THEN 200 ELSE 0 END)
                        ELSE
                            (CASE WHEN c.`author_name_cache` IN ('鏉庣櫧', '鑻忚郊', '杈涘純鐤?, '宀抽') THEN 200 ELSE 0 END)
                    END
                ) DESC,
                c.`quality_score` DESC,
                CRC32(CONCAT(r.`recommend_date`, '-', c.`work_id`))
        ) AS `sort_no`,
        CASE
            WHEN c.`has_appreciation` = 1 THEN 'appreciation'
            WHEN c.`has_annotation` = 1 THEN 'annotation'
            WHEN c.`has_translation` = 1 THEN 'translation'
            ELSE 'content'
        END AS `reason_type`,
        CASE
            WHEN c.`has_appreciation` = 1 THEN '闄勫甫璧忔瀽锛岄€傚悎娣卞叆闃呰涓庡涔犮€?
            WHEN c.`has_annotation` = 1 THEN '闄勫甫娉ㄩ噴锛岄€傚悎浣滀负瀛︿範鍏ュ彛銆?
            WHEN c.`has_translation` = 1 THEN '闄勫甫璇戞枃锛岄€傚悎蹇€熺悊瑙ｅ唴瀹广€?
            ELSE '姝ｆ枃瀹屾暣锛岄€傚悎姣忔棩娴忚銆?
        END AS `reason_text`
    FROM `daily_recommendation` r
    JOIN `tmp_daily_candidate` c
) ranked
WHERE ranked.`sort_no` <= 5;

CREATE OR REPLACE VIEW `v_daily_recommend_today` AS
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
    ON w.`work_id` = i.`work_id`
WHERE r.`recommend_date` = CURDATE()
ORDER BY i.`sort_no`;

SET FOREIGN_KEY_CHECKS = 1;

-- 妫€鏌ョず渚嬶細
-- SELECT relation_type, COUNT(*) FROM work_relation GROUP BY relation_type;
-- SELECT recommend_date, theme_name FROM daily_recommendation ORDER BY recommend_date LIMIT 10;
-- SELECT * FROM v_daily_recommend_today;

-- =========================================================
-- team business schema
-- =========================================================

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
