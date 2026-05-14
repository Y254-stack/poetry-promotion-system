package com.example.poetry.backend.follow.repository;

import com.example.poetry.backend.follow.dto.FollowListItemDto;
import java.util.List;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class UserFollowRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public UserFollowRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public int countFollowing(long followerUserId) {
        String sql = """
            SELECT COUNT(*) FROM user_follow_user
            WHERE user_id = :userId
            """;
        MapSqlParameterSource params = new MapSqlParameterSource("userId", followerUserId);
        Integer count = jdbcTemplate.queryForObject(sql, params, Integer.class);
        return count == null ? 0 : count;
    }

    public List<FollowListItemDto> listFollowing(long followerUserId, int page, int pageSize) {
        String sql = """
            SELECT
                u.user_id AS userId,
                u.username AS username,
                u.nickname AS nickname,
                DATE_FORMAT(f.created_at, '%Y-%m-%d %H:%i:%s') AS followedAt
            FROM user_follow_user f
            JOIN app_user u ON u.user_id = f.followed_user_id
            WHERE f.user_id = :userId
            ORDER BY f.created_at DESC
            LIMIT :limit OFFSET :offset
            """;

        MapSqlParameterSource params = new MapSqlParameterSource()
            .addValue("userId", followerUserId)
            .addValue("limit", pageSize)
            .addValue("offset", Math.max(page - 1, 0) * pageSize);

        return jdbcTemplate.query(sql, params, (rs, rowNum) -> new FollowListItemDto(
            rs.getLong("userId"),
            rs.getString("username"),
            rs.getString("nickname"),
            rs.getString("followedAt")
        ));
    }

    public int deleteFollow(long followerUserId, long followedUserId) {
        String sql = """
            DELETE FROM user_follow_user
            WHERE user_id = :userId AND followed_user_id = :followedUserId
            """;
        MapSqlParameterSource params = new MapSqlParameterSource()
            .addValue("userId", followerUserId)
            .addValue("followedUserId", followedUserId);
        return jdbcTemplate.update(sql, params);
    }
}
