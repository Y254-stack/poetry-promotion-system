package com.example.poetry.backend.community.repository;

import com.example.poetry.backend.community.dto.PostResponse;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class UserProfileRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public UserProfileRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 获取用户公开信息
     */
    public Optional<Map<String, Object>> getUserProfile(Long userId) {
        String sql = """
            SELECT user_id, username, nickname, avatar_url, bio, email, created_at
            FROM app_user
            WHERE user_id = :userId AND status = 1
            """;
        MapSqlParameterSource params = new MapSqlParameterSource("userId", userId);
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, params);
        return rows.stream().findFirst();
    }

    /**
     * 获取用户发布的帖子列表
     */
    public List<PostResponse> getUserPosts(Long userId, int offset, int pageSize) {
        String sql = """
            SELECT p.post_id, p.user_id, p.title, p.content_text, p.topic_tag,
                   p.view_count, p.like_count, p.comment_count, p.collect_count, p.created_at,
                   COALESCE(u.nickname, u.username) as author
            FROM community_post p
            LEFT JOIN app_user u ON p.user_id = u.user_id
            WHERE p.user_id = :userId AND p.status = 'ACTIVE'
            ORDER BY p.created_at DESC
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
     * 获取用户发布的帖子数量
     */
    public long getUserPostCount(Long userId) {
        String sql = "SELECT COUNT(*) FROM community_post WHERE user_id = :userId AND status = 'ACTIVE'";
        MapSqlParameterSource params = new MapSqlParameterSource("userId", userId);
        Long count = jdbcTemplate.queryForObject(sql, params, Long.class);
        return count != null ? count : 0;
    }

    /**
     * 获取用户获赞总数
     */
    public long getUserLikeCount(Long userId) {
        String sql = "SELECT COALESCE(SUM(like_count), 0) FROM community_post WHERE user_id = :userId AND status = 'ACTIVE'";
        MapSqlParameterSource params = new MapSqlParameterSource("userId", userId);
        Long count = jdbcTemplate.queryForObject(sql, params, Long.class);
        return count != null ? count : 0;
    }

    public Optional<String> getAvatarUrl(long userId) {
        String sql = "SELECT avatar_url FROM app_user WHERE user_id = :userId LIMIT 1";
        MapSqlParameterSource params = new MapSqlParameterSource("userId", userId);
        List<String> rows = jdbcTemplate.query(
            sql,
            params,
            (rs, rowNum) -> rs.getString("avatar_url")
        );
        return rows.stream()
            .filter(url -> url != null && !url.isBlank())
            .findFirst();
    }

    public void updateAvatarUrl(long userId, String avatarUrl) {
        String sql = "UPDATE app_user SET avatar_url = :avatarUrl WHERE user_id = :userId";
        MapSqlParameterSource params = new MapSqlParameterSource()
            .addValue("avatarUrl", avatarUrl)
            .addValue("userId", userId);
        jdbcTemplate.update(sql, params);
    }
}