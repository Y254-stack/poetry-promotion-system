package com.example.poetry.backend.community.repository;

import com.example.poetry.backend.community.dto.CommentResponse;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public class CommentRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public CommentRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 创建评论（支持一级评论和回复）
     */
    public long createComment(Long postId, Long userId, String contentText,
                              Long parentCommentId, Long replyUserId) {
        String sql = """
            INSERT INTO community_comment 
            (post_id, user_id, content_text, parent_comment_id, reply_user_id, status, created_at, updated_at)
            VALUES (:postId, :userId, :contentText, :parentCommentId, :replyUserId, 'ACTIVE', :now, :now)
            """;
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("postId", postId)
                .addValue("userId", userId)
                .addValue("contentText", contentText)
                .addValue("parentCommentId", parentCommentId)
                .addValue("replyUserId", replyUserId)
                .addValue("now", LocalDateTime.now());

        jdbcTemplate.update(sql, params);

        String idSql = "SELECT LAST_INSERT_ID()";
        Long commentId = jdbcTemplate.queryForObject(idSql, new MapSqlParameterSource(), Long.class);
        if (commentId == null) {
            throw new IllegalStateException("Failed to get created comment id");
        }
        return commentId;
    }

    /**
     * 获取帖子的评论列表（支持嵌套回复）
     * 先获取所有评论，然后在Service层组织树形结构
     */
    public List<CommentResponse> getCommentsByPostId(Long postId) {
        String sql = """
            SELECT c.comment_id, c.post_id, c.user_id, c.content_text, 
                   c.parent_comment_id, c.reply_user_id, c.created_at,
                   COALESCE(u.nickname, u.username) as author,
                   COALESCE(reply_user.nickname, reply_user.username) as reply_to_author,
                   COALESCE(cl.like_count, 0) as like_count
            FROM community_comment c
            LEFT JOIN app_user u ON c.user_id = u.user_id
            LEFT JOIN app_user reply_user ON c.reply_user_id = reply_user.user_id
            LEFT JOIN (SELECT comment_id, COUNT(*) as like_count FROM community_comment_like GROUP BY comment_id) cl 
                ON c.comment_id = cl.comment_id
            WHERE c.post_id = :postId AND c.status = 'ACTIVE'
            ORDER BY 
                COALESCE(c.parent_comment_id, c.comment_id) ASC,
                CASE WHEN c.parent_comment_id IS NULL THEN 0 ELSE 1 END ASC,
                c.created_at ASC
            """;
        MapSqlParameterSource params = new MapSqlParameterSource("postId", postId);

        return jdbcTemplate.query(sql, params, (rs, rowNum) -> new CommentResponse(
                rs.getLong("comment_id"),
                rs.getLong("post_id"),
                rs.getLong("user_id"),
                rs.getString("author"),
                rs.getString("content_text"),
                rs.getObject("parent_comment_id") != null ? rs.getLong("parent_comment_id") : null,
                rs.getObject("reply_user_id") != null ? rs.getLong("reply_user_id") : null,
                rs.getString("reply_to_author"),
                rs.getTimestamp("created_at").toLocalDateTime(),
                rs.getInt("like_count"),
                false  // isLiked 由 Service 层设置
        ));
    }

    /**
     * 获取单个评论
     */
    public CommentResponse getCommentById(Long commentId) {
        String sql = """
            SELECT c.comment_id, c.post_id, c.user_id, c.content_text, 
                   c.parent_comment_id, c.reply_user_id, c.created_at,
                   COALESCE(u.nickname, u.username) as author,
                   COALESCE(reply_user.nickname, reply_user.username) as reply_to_author,
                   COALESCE(cl.like_count, 0) as like_count
            FROM community_comment c
            LEFT JOIN app_user u ON c.user_id = u.user_id
            LEFT JOIN app_user reply_user ON c.reply_user_id = reply_user.user_id
            LEFT JOIN (SELECT comment_id, COUNT(*) as like_count FROM community_comment_like GROUP BY comment_id) cl 
                ON c.comment_id = cl.comment_id
            WHERE c.comment_id = :commentId AND c.status = 'ACTIVE'
            """;
        MapSqlParameterSource params = new MapSqlParameterSource("commentId", commentId);

        return jdbcTemplate.query(sql, params, (rs, rowNum) -> new CommentResponse(
                rs.getLong("comment_id"),
                rs.getLong("post_id"),
                rs.getLong("user_id"),
                rs.getString("author"),
                rs.getString("content_text"),
                rs.getObject("parent_comment_id") != null ? rs.getLong("parent_comment_id") : null,
                rs.getObject("reply_user_id") != null ? rs.getLong("reply_user_id") : null,
                rs.getString("reply_to_author"),
                rs.getTimestamp("created_at").toLocalDateTime(),
                rs.getInt("like_count"),
                false  // isLiked 由 Service 层设置
        )).stream().findFirst().orElse(null);
    }

    /**
     * 获取评论数量
     */
    public int getCommentCountByPostId(Long postId) {
        String sql = "SELECT COUNT(*) FROM community_comment WHERE post_id = :postId AND status = 'ACTIVE'";
        MapSqlParameterSource params = new MapSqlParameterSource("postId", postId);
        Integer count = jdbcTemplate.queryForObject(sql, params, Integer.class);
        return count != null ? count : 0;
    }

    /**
     * 获取子评论数量
     */
    public int getChildCommentCount(Long parentCommentId) {
        String sql = "SELECT COUNT(*) FROM community_comment WHERE parent_comment_id = :parentCommentId AND status = 'ACTIVE'";
        MapSqlParameterSource params = new MapSqlParameterSource("parentCommentId", parentCommentId);
        Integer count = jdbcTemplate.queryForObject(sql, params, Integer.class);
        return count != null ? count : 0;
    }
}