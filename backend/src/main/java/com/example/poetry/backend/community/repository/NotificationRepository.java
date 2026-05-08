package com.example.poetry.backend.community.repository;

import com.example.poetry.backend.community.dto.NotificationResponse;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public class NotificationRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public NotificationRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 创建通知
     */
    public long createNotification(Long userId, String type, Long actorId, Long postId, Long commentId) {
        String sql = """
            INSERT INTO community_notification (user_id, type, actor_id, post_id, comment_id, is_read, created_at)
            VALUES (:userId, :type, :actorId, :postId, :commentId, FALSE, :now)
            """;
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("userId", userId)
                .addValue("type", type)
                .addValue("actorId", actorId)
                .addValue("postId", postId)
                .addValue("commentId", commentId)
                .addValue("now", LocalDateTime.now());

        jdbcTemplate.update(sql, params);

        String idSql = "SELECT LAST_INSERT_ID()";
        Long notificationId = jdbcTemplate.queryForObject(idSql, new MapSqlParameterSource(), Long.class);
        if (notificationId == null) {
            throw new IllegalStateException("Failed to get created notification id");
        }
        return notificationId;
    }

    /**
     * 获取用户的通知列表
     */
    public List<NotificationResponse> getNotifications(Long userId, int offset, int pageSize) {
        String sql = """
            SELECT n.notification_id, n.user_id, n.type, n.actor_id, n.post_id, n.comment_id, n.is_read, n.created_at,
                   COALESCE(u.nickname, u.username) as actor_name,
                   p.title as post_title,
                   c.content_text as comment_content
            FROM community_notification n
            LEFT JOIN app_user u ON n.actor_id = u.user_id
            LEFT JOIN community_post p ON n.post_id = p.post_id
            LEFT JOIN community_comment c ON n.comment_id = c.comment_id
            WHERE n.user_id = :userId
            ORDER BY n.created_at DESC
            LIMIT :offset, :pageSize
            """;
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("userId", userId)
                .addValue("offset", offset)
                .addValue("pageSize", pageSize);

        return jdbcTemplate.query(sql, params, (rs, rowNum) -> {
            String commentContent = rs.getString("comment_content");
            String preview = commentContent != null && commentContent.length() > 50
                    ? commentContent.substring(0, 50) + "..."
                    : commentContent;
            return new NotificationResponse(
                    rs.getLong("notification_id"),
                    rs.getLong("user_id"),
                    rs.getString("type"),
                    rs.getLong("actor_id"),
                    rs.getString("actor_name"),
                    rs.getLong("post_id"),
                    rs.getString("post_title"),
                    rs.getObject("comment_id") != null ? rs.getLong("comment_id") : null,
                    preview,
                    rs.getBoolean("is_read"),
                    rs.getTimestamp("created_at").toLocalDateTime()
            );
        });
    }

    /**
     * 获取用户通知总数
     */
    public long getNotificationCount(Long userId) {
        String sql = "SELECT COUNT(*) FROM community_notification WHERE user_id = :userId";
        MapSqlParameterSource params = new MapSqlParameterSource("userId", userId);
        Long count = jdbcTemplate.queryForObject(sql, params, Long.class);
        return count != null ? count : 0;
    }

    /**
     * 获取用户未读通知数量
     */
    public long getUnreadCount(Long userId) {
        String sql = "SELECT COUNT(*) FROM community_notification WHERE user_id = :userId AND is_read = FALSE";
        MapSqlParameterSource params = new MapSqlParameterSource("userId", userId);
        Long count = jdbcTemplate.queryForObject(sql, params, Long.class);
        return count != null ? count : 0;
    }

    /**
     * 标记单条通知为已读
     */
    public boolean markAsRead(Long notificationId, Long userId) {
        String sql = "UPDATE community_notification SET is_read = TRUE WHERE notification_id = :notificationId AND user_id = :userId";
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("notificationId", notificationId)
                .addValue("userId", userId);
        int affected = jdbcTemplate.update(sql, params);
        return affected > 0;
    }

    /**
     * 标记所有通知为已读
     */
    public boolean markAllAsRead(Long userId) {
        String sql = "UPDATE community_notification SET is_read = TRUE WHERE user_id = :userId AND is_read = FALSE";
        MapSqlParameterSource params = new MapSqlParameterSource("userId", userId);
        int affected = jdbcTemplate.update(sql, params);
        return affected > 0;
    }

    /**
     * 删除单条通知
     */
    public boolean deleteNotification(Long notificationId, Long userId) {
        String sql = "DELETE FROM community_notification WHERE notification_id = :notificationId AND user_id = :userId";
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("notificationId", notificationId)
                .addValue("userId", userId);
        int affected = jdbcTemplate.update(sql, params);
        return affected > 0;
    }

    /**
     * 删除所有通知
     */
    public boolean deleteAllNotifications(Long userId) {
        String sql = "DELETE FROM community_notification WHERE user_id = :userId";
        MapSqlParameterSource params = new MapSqlParameterSource("userId", userId);
        int affected = jdbcTemplate.update(sql, params);
        return affected > 0;
    }
}