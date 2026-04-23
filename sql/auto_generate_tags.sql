SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

USE `poetry_clean`;

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

INSERT IGNORE INTO `tag` (`tag_name`, `tag_type`, `description`) VALUES
('春天', 'season', '与春景、春意、春情相关'),
('夏天', 'season', '与夏景、荷花、暑气相关'),
('秋天', 'season', '与秋景、秋思、霜叶相关'),
('冬天', 'season', '与冬雪、寒梅、冰霜相关'),
('月亮', 'image', '与月色、明月、婵娟相关'),
('山水', 'scene', '与山川江河湖海相关'),
('田园', 'scene', '与村居、农耕、归隐相关'),
('边塞', 'scene', '与边关、征戍、塞外相关'),
('送别', 'theme', '与送别、离别、赠行相关'),
('思乡', 'theme', '与故乡、家书、乡愁相关'),
('怀古', 'theme', '与咏史、怀古、古迹相关'),
('战争', 'theme', '与军旅、烽火、征战相关'),
('爱情', 'emotion', '与相思、闺情、爱恋相关'),
('咏物', 'theme', '与咏物、赋物、题物相关'),
('写景', 'theme', '与自然景物描写相关'),
('哲理', 'theme', '与人生感悟、哲思相关'),
('爱国', 'theme', '与忧国、报国、家国相关'),
('重阳', 'festival', '与重阳节相关'),
('七夕', 'festival', '与七夕节相关'),
('中秋', 'festival', '与中秋节相关'),
('清明', 'festival', '与清明节相关'),
('元宵', 'festival', '与元宵节相关');

DROP TEMPORARY TABLE IF EXISTS `tmp_auto_tags`;
CREATE TEMPORARY TABLE `tmp_auto_tags` (
    `work_id` BIGINT UNSIGNED NOT NULL,
    `tag_name` VARCHAR(64) COLLATE utf8mb4_unicode_ci NOT NULL,
    `weight` DECIMAL(6,3) NOT NULL DEFAULT 1.000,
    PRIMARY KEY (`work_id`, `tag_name`)
) ENGINE=InnoDB;

INSERT IGNORE INTO `tmp_auto_tags` (`work_id`, `tag_name`, `weight`)
SELECT `work_id`, '春天', 1.000
FROM `poetry_work`
WHERE CONCAT(IFNULL(`title`, ''), ' ', IFNULL(`content_text`, '')) REGEXP '春|桃花|杨柳|燕子|芳草|花开';

INSERT IGNORE INTO `tmp_auto_tags` (`work_id`, `tag_name`, `weight`)
SELECT `work_id`, '夏天', 1.000
FROM `poetry_work`
WHERE CONCAT(IFNULL(`title`, ''), ' ', IFNULL(`content_text`, '')) REGEXP '夏|荷|莲|蝉|暑|炎';

INSERT IGNORE INTO `tmp_auto_tags` (`work_id`, `tag_name`, `weight`)
SELECT `work_id`, '秋天', 1.000
FROM `poetry_work`
WHERE CONCAT(IFNULL(`title`, ''), ' ', IFNULL(`content_text`, '')) REGEXP '秋|菊|霜|雁|枫|落叶';

INSERT IGNORE INTO `tmp_auto_tags` (`work_id`, `tag_name`, `weight`)
SELECT `work_id`, '冬天', 1.000
FROM `poetry_work`
WHERE CONCAT(IFNULL(`title`, ''), ' ', IFNULL(`content_text`, '')) REGEXP '冬|雪|梅|寒|冰';

INSERT IGNORE INTO `tmp_auto_tags` (`work_id`, `tag_name`, `weight`)
SELECT `work_id`, '月亮', 1.000
FROM `poetry_work`
WHERE CONCAT(IFNULL(`title`, ''), ' ', IFNULL(`content_text`, '')) REGEXP '月|婵娟|明月|残月|秋月';

INSERT IGNORE INTO `tmp_auto_tags` (`work_id`, `tag_name`, `weight`)
SELECT `work_id`, '山水', 0.900
FROM `poetry_work`
WHERE CONCAT(IFNULL(`title`, ''), ' ', IFNULL(`content_text`, '')) REGEXP '山|江|河|湖|海|溪|泉|水|漓江|长河';

INSERT IGNORE INTO `tmp_auto_tags` (`work_id`, `tag_name`, `weight`)
SELECT `work_id`, '田园', 0.950
FROM `poetry_work`
WHERE CONCAT(IFNULL(`title`, ''), ' ', IFNULL(`content_text`, '')) REGEXP '田|园|村|农|耕|柴门|归隐|山村|田家';

INSERT IGNORE INTO `tmp_auto_tags` (`work_id`, `tag_name`, `weight`)
SELECT `work_id`, '边塞', 1.000
FROM `poetry_work`
WHERE CONCAT(IFNULL(`title`, ''), ' ', IFNULL(`content_text`, '')) REGEXP '塞|边|胡|羌|戍|关山|玉门|楼兰|瀚海';

INSERT IGNORE INTO `tmp_auto_tags` (`work_id`, `tag_name`, `weight`)
SELECT `work_id`, '送别', 1.000
FROM `poetry_work`
WHERE CONCAT(IFNULL(`title`, ''), ' ', IFNULL(`content_text`, '')) REGEXP '送|别|离|赠|饯|留别';

INSERT IGNORE INTO `tmp_auto_tags` (`work_id`, `tag_name`, `weight`)
SELECT `work_id`, '思乡', 1.000
FROM `poetry_work`
WHERE CONCAT(IFNULL(`title`, ''), ' ', IFNULL(`content_text`, '')) REGEXP '乡|故园|故乡|乡愁|家书|归梦|归家';

INSERT IGNORE INTO `tmp_auto_tags` (`work_id`, `tag_name`, `weight`)
SELECT `work_id`, '怀古', 1.000
FROM `poetry_work`
WHERE CONCAT(IFNULL(`title`, ''), ' ', IFNULL(`content_text`, '')) REGEXP '怀古|咏史|赤壁|金陵|古迹|故垒|乌衣巷|台城';

INSERT IGNORE INTO `tmp_auto_tags` (`work_id`, `tag_name`, `weight`)
SELECT `work_id`, '战争', 1.000
FROM `poetry_work`
WHERE CONCAT(IFNULL(`title`, ''), ' ', IFNULL(`content_text`, '')) REGEXP '战|兵|军|烽火|征|戎|甲兵|将军';

INSERT IGNORE INTO `tmp_auto_tags` (`work_id`, `tag_name`, `weight`)
SELECT `work_id`, '爱情', 1.000
FROM `poetry_work`
WHERE CONCAT(IFNULL(`title`, ''), ' ', IFNULL(`content_text`, '')) REGEXP '相思|伊人|红豆|闺|情|鸳鸯|郎|妾|罗裙';

INSERT IGNORE INTO `tmp_auto_tags` (`work_id`, `tag_name`, `weight`)
SELECT `work_id`, '咏物', 0.950
FROM `poetry_work`
WHERE IFNULL(`title`, '') REGEXP '咏|赋|题.*(梅|兰|竹|菊|柳|荷|月|雪|石|松|鹰|马|雁|兰)'
   OR CONCAT(IFNULL(`title`, ''), ' ', IFNULL(`content_text`, '')) REGEXP '咏梅|咏柳|咏竹|咏兰';

INSERT IGNORE INTO `tmp_auto_tags` (`work_id`, `tag_name`, `weight`)
SELECT `work_id`, '写景', 0.800
FROM `poetry_work`
WHERE CONCAT(IFNULL(`title`, ''), ' ', IFNULL(`content_text`, '')) REGEXP '山|水|月|风|云|雨|雪|花|草|柳|日|夜';

INSERT IGNORE INTO `tmp_auto_tags` (`work_id`, `tag_name`, `weight`)
SELECT `work_id`, '哲理', 0.850
FROM `poetry_work`
WHERE CONCAT(IFNULL(`title`, ''), ' ', IFNULL(`content_text`, '')) REGEXP '人生|世事|浮生|有无|得失|心境|空|悟|理';

INSERT IGNORE INTO `tmp_auto_tags` (`work_id`, `tag_name`, `weight`)
SELECT `work_id`, '爱国', 0.950
FROM `poetry_work`
WHERE CONCAT(IFNULL(`title`, ''), ' ', IFNULL(`content_text`, '')) REGEXP '报国|忧国|山河|社稷|国耻|恢复中原|收复'
   OR IFNULL(`author_name_cache`, '') IN ('陆游', '辛弃疾', '文天祥', '岳飞', '龚自珍');

INSERT IGNORE INTO `tmp_auto_tags` (`work_id`, `tag_name`, `weight`)
SELECT `work_id`, '重阳', 1.000
FROM `poetry_work`
WHERE CONCAT(IFNULL(`title`, ''), ' ', IFNULL(`content_text`, '')) REGEXP '重阳|九日|登高';

INSERT IGNORE INTO `tmp_auto_tags` (`work_id`, `tag_name`, `weight`)
SELECT `work_id`, '七夕', 1.000
FROM `poetry_work`
WHERE CONCAT(IFNULL(`title`, ''), ' ', IFNULL(`content_text`, '')) REGEXP '七夕|乞巧|牛郎|织女|鹊桥';

INSERT IGNORE INTO `tmp_auto_tags` (`work_id`, `tag_name`, `weight`)
SELECT `work_id`, '中秋', 1.000
FROM `poetry_work`
WHERE CONCAT(IFNULL(`title`, ''), ' ', IFNULL(`content_text`, '')) REGEXP '中秋|八月十五|婵娟|明月几时有';

INSERT IGNORE INTO `tmp_auto_tags` (`work_id`, `tag_name`, `weight`)
SELECT `work_id`, '清明', 1.000
FROM `poetry_work`
WHERE CONCAT(IFNULL(`title`, ''), ' ', IFNULL(`content_text`, '')) REGEXP '清明|寒食';

INSERT IGNORE INTO `tmp_auto_tags` (`work_id`, `tag_name`, `weight`)
SELECT `work_id`, '元宵', 1.000
FROM `poetry_work`
WHERE CONCAT(IFNULL(`title`, ''), ' ', IFNULL(`content_text`, '')) REGEXP '元宵|上元|灯市|花灯';

DELETE wt
FROM `work_tag` wt
JOIN `tag` t ON t.`tag_id` = wt.`tag_id`
WHERE t.`tag_name` IN (
    '春天','夏天','秋天','冬天',
    '月亮','山水','田园','边塞',
    '送别','思乡','怀古','战争','爱情','咏物','写景','哲理','爱国',
    '重阳','七夕','中秋','清明','元宵'
);

INSERT INTO `work_tag` (`work_id`, `tag_id`, `weight`)
SELECT a.`work_id`, t.`tag_id`, a.`weight`
FROM `tmp_auto_tags` a
JOIN `tag` t
  ON t.`tag_name` COLLATE utf8mb4_unicode_ci = a.`tag_name` COLLATE utf8mb4_unicode_ci;

DROP VIEW IF EXISTS `v_hot_tags`;
CREATE VIEW `v_hot_tags` AS
SELECT
    t.`tag_id`,
    t.`tag_name`,
    t.`tag_type`,
    COUNT(wt.`work_id`) AS `work_count`,
    ROUND(AVG(wt.`weight`), 3) AS `avg_weight`
FROM `tag` t
LEFT JOIN `work_tag` wt ON wt.`tag_id` = t.`tag_id`
GROUP BY t.`tag_id`, t.`tag_name`, t.`tag_type`
ORDER BY `work_count` DESC, `avg_weight` DESC;

SET FOREIGN_KEY_CHECKS = 1;
