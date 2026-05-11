package com.example.poetry.backend.community.repository;

import com.example.poetry.backend.community.dto.PostResponse;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class CommunityLikeRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public CommunityLikeRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 点赞帖子
     */
    public boolean likePost(Long userId, Long postId) {
        String sql = """
            INSERT INTO community_post_like (post_id, user_id, created_at)
            VALUES (:postId, :userId, CURRENT_TIMESTAMP)
            ON DUPLICATE KEY UPDATE created_at = CURRENT_TIMESTAMP
            """;
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("postId", postId)
                .addValue("userId", userId);
        int affected = jdbcTemplate.update(sql, params);
        return affected > 0;
    }

    /**
     * 取消点赞
     */
    public boolean unlikePost(Long userId, Long postId) {
        String sql = "DELETE FROM community_post_like WHERE post_id = :postId AND user_id = :userId";
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("postId", postId)
                .addValue("userId", userId);
        int affected = jdbcTemplate.update(sql, params);
        return affected > 0;
    }

    /**
     * 检查是否已点赞
     */
    public boolean isLiked(Long userId, Long postId) {
        String sql = "SELECT COUNT(*) FROM community_post_like WHERE post_id = :postId AND user_id = :userId";
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("postId", postId)
                .addValue("userId", userId);
        Integer count = jdbcTemplate.queryForObject(sql, params, Integer.class);
        return count != null && count > 0;
    }

    /**
     * 获取用户喜欢的帖子列表
     */
    public List<PostResponse> getLikedPosts(Long userId, int offset, int pageSize) {
        String sql = """
            SELECT p.post_id, p.user_id, p.title, p.content_text, p.topic_tag,
                   p.view_count, p.like_count, p.comment_count, p.collect_count, p.created_at,
                   COALESCE(u.nickname, u.username) as author
            FROM community_post_like pl
            JOIN community_post p ON pl.post_id = p.post_id
            LEFT JOIN app_user u ON p.user_id = u.user_id
            WHERE pl.user_id = :userId AND p.status = 'ACTIVE'
            ORDER BY pl.created_at DESC
            LIMIT :offset, :pageSize
            """;
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("userId", userId)
                .addValue("offset", offset)
                .addValue("pageSize", pageSize);

        return jdbcTemplate.query(sql, params, (rs, rowNum) -> {
            String contentText = rs.getString("content_text");
            String preview = contentText != null && contentText.length() > 50
                    ? contentText.substring(0, 50) + "..."
                    : contentText;
            return new PostResponse(
                    rs.getLong("post_id"),
                    rs.getLong("user_id"),
                    rs.getString("author"),
                    rs.getString("title"),
                    preview,
                    rs.getString("topic_tag"),
                    rs.getInt("view_count"),
                    rs.getInt("like_count"),
                    rs.getInt("comment_count"),
                    rs.getInt("collect_count"),
                    rs.getTimestamp("created_at").toLocalDateTime()
            );
        });
    }

    /**
     * 获取用户喜欢的帖子数量
     */
    public long getLikedPostCount(Long userId) {
        String sql = "SELECT COUNT(*) FROM community_post_like WHERE user_id = :userId";
        MapSqlParameterSource params = new MapSqlParameterSource("userId", userId);
        Long count = jdbcTemplate.queryForObject(sql, params, Long.class);
        return count != null ? count : 0;
    }
}