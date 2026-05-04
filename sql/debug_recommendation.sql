-- =========================================================
-- 调试推荐数据导入问题
-- =========================================================
SET NAMES utf8mb4;
USE `poetry_clean`;

-- 1. 检查基础数据是否存在
SELECT 'poetry_work' AS table_name, COUNT(*) AS record_count FROM poetry_work;
SELECT 'author' AS table_name, COUNT(*) AS record_count FROM author;
SELECT 'work_sentence' AS table_name, COUNT(*) AS record_count FROM work_sentence;

-- 2. 检查author_id填充情况
SELECT 
    'author_id统计' AS stat,
    COUNT(*) AS total,
    SUM(CASE WHEN author_id IS NULL THEN 1 ELSE 0 END) AS null_count,
    SUM(CASE WHEN author_id IS NOT NULL THEN 1 ELSE 0 END) AS not_null_count
FROM poetry_work;

-- 3. 检查dynasty_name分布
SELECT dynasty_name, COUNT(*) AS count 
FROM poetry_work 
WHERE dynasty_name IS NOT NULL AND dynasty_name <> ''
GROUP BY dynasty_name 
ORDER BY count DESC 
LIMIT 20;

-- 4. 检查quality_score计算
SELECT 
    MIN(quality_score) AS min_score,
    MAX(quality_score) AS max_score,
    AVG(quality_score) AS avg_score,
    COUNT(*) AS total,
    SUM(CASE WHEN quality_score >= 20 THEN 1 ELSE 0 END) AS qualified_count
FROM (
    SELECT 
        w.work_id,
        (
            CASE WHEN w.translation_text IS NOT NULL AND w.translation_text <> '' THEN 30 ELSE 0 END +
            CASE WHEN w.annotation_text IS NOT NULL AND w.annotation_text <> '' THEN 25 ELSE 0 END +
            CASE WHEN w.appreciation_text IS NOT NULL AND w.appreciation_text <> '' THEN 20 ELSE 0 END +
            CASE WHEN w.char_count BETWEEN 12 AND 200 THEN 10 ELSE 0 END +
            CASE WHEN w.dynasty_name IN ('唐代', '宋代') THEN 8 ELSE 0 END +
            CASE WHEN w.author_name_cache IN ('李白', '杜甫', '白居易', '苏轼', '辛弃疾', '李清照', '王维', '李商隐', '杜牧', '陶渊明') THEN 12 ELSE 0 END
        ) AS quality_score
    FROM poetry_work w
) tmp;

-- 5. 检查links_raw字段是否有数据
SELECT 
    'links_raw统计' AS stat,
    COUNT(*) AS total,
    SUM(CASE WHEN links_raw IS NOT NULL AND links_raw <> '' AND links_raw <> '[]' THEN 1 ELSE 0 END) AS has_links_count
FROM poetry_work;

-- 6. 检查现有推荐数据
SELECT 'work_relation' AS table_name, COUNT(*) AS record_count FROM work_relation;
SELECT 'daily_recommendation' AS table_name, COUNT(*) AS record_count FROM daily_recommendation;
SELECT 'daily_recommend_item' AS table_name, COUNT(*) AS record_count FROM daily_recommend_item;

-- 7. 如果work_relation为空，检查可能的问题
SELECT COUNT(*) AS same_author_possible 
FROM poetry_work w1 
JOIN poetry_work w2 ON w1.author_id = w2.author_id AND w1.work_id <> w2.work_id 
WHERE w1.author_id IS NOT NULL;

SELECT COUNT(*) AS same_dynasty_possible 
FROM poetry_work w1 
JOIN poetry_work w2 ON w1.dynasty_name = w2.dynasty_name AND w1.work_id <> w2.work_id 
WHERE w1.dynasty_name IS NOT NULL AND w1.dynasty_name <> '';