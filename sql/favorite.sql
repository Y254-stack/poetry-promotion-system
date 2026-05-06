-- 用户收藏诗词表
CREATE TABLE IF NOT EXISTS user_favorite_poem (
    favorite_id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT '收藏记录ID',
    user_id BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
    work_id BIGINT UNSIGNED NOT NULL COMMENT '诗词作品ID',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '收藏时间',
    UNIQUE KEY uk_user_work (user_id, work_id) COMMENT '用户和作品的唯一索引',
    KEY idx_user_id (user_id) COMMENT '用户ID索引',
    KEY idx_work_id (work_id) COMMENT '作品ID索引',
    KEY idx_created_at (created_at) COMMENT '收藏时间索引'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户收藏诗词表';
