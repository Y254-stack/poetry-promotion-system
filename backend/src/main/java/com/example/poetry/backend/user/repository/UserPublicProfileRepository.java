package com.example.poetry.backend.user.repository;

import com.example.poetry.backend.user.dto.UserPublicProfileResponse;
import com.example.poetry.backend.user.dto.UserPublishedPostItemDto;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class UserPublicProfileRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public UserPublicProfileRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<UserPublicProfileResponse> findPublicProfile(long userId) {
        String sql = """
            SELECT user_id AS userId, username, nickname
            FROM app_user
            WHERE user_id = :userId
            """;
        MapSqlParameterSource params = new MapSqlParameterSource("userId", userId);
        List<UserPublicProfileResponse> rows = jdbcTemplate.query(sql, params, (rs, rowNum) ->
            new UserPublicProfileResponse(
                rs.getLong("userId"),
                rs.getString("username"),
                rs.getString("nickname")
            )
        );
        if (rows.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(rows.get(0));
    }

    public int countPublishedPosts(long userId) {
        String sql = """
            SELECT COUNT(*) FROM community_post
            WHERE user_id = :userId AND status = 'published'
            """;
        MapSqlParameterSource params = new MapSqlParameterSource("userId", userId);
        Integer count = jdbcTemplate.queryForObject(sql, params, Integer.class);
        return count == null ? 0 : count;
    }

    public List<UserPublishedPostItemDto> listPublishedPosts(long userId, int page, int pageSize) {
        String sql = """
            SELECT
                p.post_id AS postId,
                p.title,
                p.topic_tag AS topicTag,
                LEFT(COALESCE(p.content_text, ''), 120) AS contentPreview,
                DATE_FORMAT(p.created_at, '%Y-%m-%d %H:%i:%s') AS publishedAt
            FROM community_post p
            WHERE p.user_id = :userId AND p.status = 'published'
            ORDER BY p.created_at DESC
            LIMIT :limit OFFSET :offset
            """;

        MapSqlParameterSource params = new MapSqlParameterSource()
            .addValue("userId", userId)
            .addValue("limit", pageSize)
            .addValue("offset", Math.max(page - 1, 0) * pageSize);

        return jdbcTemplate.query(sql, params, (rs, rowNum) -> new UserPublishedPostItemDto(
            rs.getLong("postId"),
            rs.getString("title"),
            rs.getString("topicTag"),
            rs.getString("contentPreview"),
            rs.getString("publishedAt")
        ));
    }
}
