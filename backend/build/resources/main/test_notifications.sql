-- 根据实际数据生成的测试通知
-- 用户ID: 1, 2, 3, 4, 5, 6, 7, 8
-- 帖子ID: 1

INSERT INTO community_notification (user_id, type, actor_id, post_id, comment_id, is_read, created_at) VALUES
(1, 'COMMENT', 2, 1, NULL, FALSE, NOW() - INTERVAL 10 MINUTE),
(1, 'LIKE', 3, 1, NULL, FALSE, NOW() - INTERVAL 1 HOUR),
(1, 'COLLECT', 4, 1, NULL, FALSE, NOW() - INTERVAL 2 HOUR),
(1, 'COMMENT', 5, 1, NULL, TRUE, NOW() - INTERVAL 1 DAY),
(1, 'LIKE', 6, 1, NULL, TRUE, NOW() - INTERVAL 2 DAY);

-- 验证插入结果
SELECT * FROM community_notification WHERE user_id = 1 ORDER BY created_at DESC;
