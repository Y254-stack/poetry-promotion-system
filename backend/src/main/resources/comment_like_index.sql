-- 确保 community_comment_like 表有唯一索引（防止重复点赞）
ALTER TABLE community_comment_like 
ADD UNIQUE INDEX uk_comment_user (comment_id, user_id);

-- 查看表结构确认
DESCRIBE community_comment_like;
