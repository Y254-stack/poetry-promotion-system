-- =========================================================
-- 修复推荐数据导入问题
-- 解决 work_relation, daily_recommendation, daily_recommend_item 数据为空的问题
-- =========================================================
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

USE `poetry_clean`;

-- =========================================================
-- 第一步：修复可能的关联问题
-- =========================================================

-- 1. 确保author表与poetry_work表正确关联
-- 如果author_id为空，尝试重新匹配
UPDATE poetry_work w
LEFT JOIN author a 
    ON a.canonical_name = w.author_name_cache
    AND (a.dynasty_name = w.dynasty_name OR (a.dynasty_name IS NULL AND w.dynasty_name IS NULL))
SET w.author_id = a.author_id
WHERE w.author_id IS NULL 
  AND w.author_name_cache IS NOT NULL 
  AND w.author_name_cache <> '';

-- 2. 处理dynasty_name可能的乱码问题
UPDATE poetry_work 
SET dynasty_name = CASE 
    WHEN dynasty_name LIKE '%唐%' THEN '唐代'
    WHEN dynasty_name LIKE '%宋%' THEN '宋代'
    WHEN dynasty_name LIKE '%明%' THEN '明代'
    WHEN dynasty_name LIKE '%清%' THEN '清代'
    WHEN dynasty_name LIKE '%汉%' THEN '汉代'
    WHEN dynasty_name LIKE '%三国%' THEN '三国'
    WHEN dynasty_name LIKE '%魏晋%' THEN '魏晋'
    WHEN dynasty_name LIKE '%南北朝%' THEN '南北朝'
    WHEN dynasty_name LIKE '%隋%' THEN '隋代'
    WHEN dynasty_name LIKE '%五代%' THEN '五代'
    WHEN dynasty_name LIKE '%元%' THEN '元代'
    ELSE dynasty_name 
END
WHERE dynasty_name IS NOT NULL;

-- =========================================================
-- 第二步：重新创建推荐表并导入数据
-- =========================================================

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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='作品相关推荐表';

CREATE TABLE `daily_recommendation` (
    `recommendation_id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `recommend_date` DATE NOT NULL,
    `theme_name` VARCHAR(64) NOT NULL,
    `intro_text` VARCHAR(255) DEFAULT NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`recommendation_id`),
    UNIQUE KEY `uk_daily_recommend_date` (`recommend_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='每日推荐主表';

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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='每日推荐明细表';

-- =========================================================
-- 第三步：创建质量评分临时表（放宽条件）
-- =========================================================

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
        CASE WHEN w.`char_count` BETWEEN 8 AND 500 THEN 10 ELSE 0 END +
        CASE WHEN w.`dynasty_name` IN ('唐代', '宋代', '唐朝', '宋朝', '唐', '宋') THEN 8 ELSE 0 END +
        CASE WHEN w.`author_name_cache` IN ('李白', '杜甫', '白居易', '苏轼', '辛弃疾', '李清照', '王维', '李商隐', '杜牧', '陶渊明', '陆游', '孟浩然', '王昌龄', '王之涣')
             THEN 12 ELSE 0 END
    ) AS `quality_score`
FROM `poetry_work` w;

-- =========================================================
-- 第四步：导入同作者推荐（放宽条件）
-- =========================================================

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
    '同作者相邻作品' AS `source_note`
FROM `tmp_author_rank` a
JOIN `tmp_author_rank_b` b
    ON a.`author_id` = b.`author_id`
   AND a.`work_id` <> b.`work_id`
   AND ABS(CAST(a.`rn` AS SIGNED) - CAST(b.`rn` AS SIGNED)) BETWEEN 1 AND 5;

-- =========================================================
-- 第五步：导入同朝代推荐（放宽条件）
-- =========================================================

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
    '同朝代相邻作品' AS `source_note`
FROM `tmp_dynasty_rank` a
JOIN `tmp_dynasty_rank_b` b
    ON a.`dynasty_name` = b.`dynasty_name`
   AND a.`work_id` <> b.`work_id`
   AND ABS(CAST(a.`rn` AS SIGNED) - CAST(b.`rn` AS SIGNED)) BETWEEN 1 AND 3;

-- =========================================================
-- 第六步：导入每日推荐主表（生成30天）
-- =========================================================

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
        WHEN 0 THEN '春日诗意'
        WHEN 1 THEN '山水清音'
        WHEN 2 THEN '明月相思'
        WHEN 3 THEN '家国情怀'
        WHEN 4 THEN '送别离愁'
        WHEN 5 THEN '田园闲适'
        ELSE '豪放气象'
    END AS `theme_name`,
    CASE MOD(`offset_day`, 7)
        WHEN 0 THEN '围绕春意主题推荐适合碎片化阅读的经典诗词。'
        WHEN 1 THEN '聚焦山水意象，适合浏览与卡片创作。'
        WHEN 2 THEN '以明月、思念相关作品为主，适合夜间阅读。'
        WHEN 3 THEN '优先推荐家国边塞主题名篇。'
        WHEN 4 THEN '精选送别与离愁主题作品。'
        WHEN 5 THEN '推荐田园与归隐气质较强的作品。'
        ELSE '展示更具豪放气质和传播性的经典篇目。'
    END AS `intro_text`
FROM `tmp_date_series`;

-- =========================================================
-- 第七步：导入每日推荐明细（放宽条件）
-- =========================================================

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
  AND w.`char_count` BETWEEN 8 AND 500
  AND q.`quality_score` >= 5;  -- 放宽条件：从20降到5

-- 如果候选池为空，直接使用所有作品
SELECT COUNT(*) INTO @candidate_count FROM tmp_daily_candidate;
IF @candidate_count = 0 THEN
    INSERT INTO tmp_daily_candidate (
        `work_id`, `title`, `author_name_cache`, `dynasty_name`, 
        `quality_score`, `has_translation`, `has_annotation`, `has_appreciation`
    )
    SELECT 
        w.work_id, w.title, w.author_name_cache, w.dynasty_name,
        10 AS quality_score, 0, 0, 0
    FROM poetry_work w
    WHERE w.content_text IS NOT NULL AND w.content_text <> '';
END IF;

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
                        WHEN '春日诗意' THEN
                            (CASE WHEN c.`title` LIKE '%春%' OR c.`title` LIKE '%花%' THEN 200 ELSE 0 END)
                        WHEN '山水清音' THEN
                            (CASE WHEN c.`title` LIKE '%山%' OR c.`title` LIKE '%水%' THEN 200 ELSE 0 END)
                        WHEN '明月相思' THEN
                            (CASE WHEN c.`title` LIKE '%月%' OR c.`title` LIKE '%思%' THEN 200 ELSE 0 END)
                        WHEN '家国情怀' THEN
                            (CASE WHEN c.`title` LIKE '%国%' OR c.`title` LIKE '%塞%' OR c.`title` LIKE '%边%' THEN 200 ELSE 0 END)
                        WHEN '送别离愁' THEN
                            (CASE WHEN c.`title` LIKE '%送%' OR c.`title` LIKE '%别%' OR c.`title` LIKE '%愁%' THEN 200 ELSE 0 END)
                        WHEN '田园闲适' THEN
                            (CASE WHEN c.`title` LIKE '%田%' OR c.`title` LIKE '%园%' OR c.`title` LIKE '%归%' THEN 200 ELSE 0 END)
                        ELSE
                            (CASE WHEN c.`author_name_cache` IN ('李白', '苏轼', '辛弃疾', '岳飞', '杜甫') THEN 200 ELSE 0 END)
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
            WHEN c.`has_appreciation` = 1 THEN '附带赏析，适合深入阅读与学习。'
            WHEN c.`has_annotation` = 1 THEN '附带注释，适合作为学习入口。'
            WHEN c.`has_translation` = 1 THEN '附带译文，适合快速理解内容。'
            ELSE '正文完整，适合每日浏览。'
        END AS `reason_text`
    FROM `daily_recommendation` r
    JOIN `tmp_daily_candidate` c
) ranked
WHERE ranked.`sort_no` <= 5;

-- =========================================================
-- 第八步：创建视图
-- =========================================================

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

-- =========================================================
-- 验证结果
-- =========================================================
SELECT 'work_relation' AS table_name, COUNT(*) AS record_count FROM work_relation;
SELECT 'daily_recommendation' AS table_name, COUNT(*) AS record_count FROM daily_recommendation;
SELECT 'daily_recommend_item' AS table_name, COUNT(*) AS record_count FROM daily_recommend_item;
SELECT * FROM v_daily_recommend_today LIMIT 5;