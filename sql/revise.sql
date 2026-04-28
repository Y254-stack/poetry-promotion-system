---- 删除三个无用表


-- 删除点赞表（无点赞功能）
DROP TABLE IF EXISTS `community_post_like`;

-- 删除作者相关的收藏/关注表（不做收藏作者、关注作者的功能）
DROP TABLE IF EXISTS `user_favorite_author`;
DROP TABLE IF EXISTS `user_follow_author`;