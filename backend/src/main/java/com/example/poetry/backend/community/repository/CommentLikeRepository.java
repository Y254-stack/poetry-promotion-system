package com.example.poetry.backend.community.repository;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public class CommentLikeRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public CommentLikeRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 点赞评论
     */
    public void likeComment(Long userId, Long commentId) {
        String sql = """
            INSERT INTO community_comment_like 
            (comment_id, user_id, created_at)
            VALUES (:commentId, :userId, :now)
            ON DUPLICATE KEY UPDATE created_at = :now
            """;
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("commentId", commentId)
                .addValue("userId", userId)
                .addValue("now", LocalDateTime.now());
        jdbcTemplate.update(sql, params);
    }

    /**
     * 取消点赞评论
     */
    public void unlikeComment(Long userId, Long commentId) {
        String sql = "DELETE FROM community_comment_like WHERE comment_id = :commentId AND user_id = :userId";
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("commentId", commentId)
                .addValue("userId", userId);
        jdbcTemplate.update(sql, params);
    }

    /**
     * 检查用户是否已点赞评论
     */
    public boolean isLiked(Long userId, Long commentId) {
        String sql = "SELECT COUNT(*) FROM community_comment_like WHERE comment_id = :commentId AND user_id = :userId";
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("commentId", commentId)
                .addValue("userId", userId);
        Integer count = jdbcTemplate.queryForObject(sql, params, Integer.class);
        return count != null && count > 0;
    }

    /**
     * 获取评论的点赞数
     */
    public int getLikeCount(Long commentId) {
        String sql = "SELECT COUNT(*) FROM community_comment_like WHERE comment_id = :commentId";
        MapSqlParameterSource params = new MapSqlParameterSource("commentId", commentId);
        Integer count = jdbcTemplate.queryForObject(sql, params, Integer.class);
        return count != null ? count : 0;
    }
}
