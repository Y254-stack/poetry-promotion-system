package com.example.poetry.backend.community.repository;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public class FollowRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public FollowRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 关注用户
     */
    public boolean followUser(Long userId, Long followedUserId) {
        String sql = """
            INSERT INTO user_follow_user (user_id, followed_user_id, created_at)
            VALUES (:userId, :followedUserId, CURRENT_TIMESTAMP)
            ON DUPLICATE KEY UPDATE created_at = CURRENT_TIMESTAMP
            """;
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("userId", userId)
                .addValue("followedUserId", followedUserId);
        int affected = jdbcTemplate.update(sql, params);
        return affected > 0;
    }

    /**
     * 取消关注用户
     */
    public boolean unfollowUser(Long userId, Long followedUserId) {
        String sql = "DELETE FROM user_follow_user WHERE user_id = :userId AND followed_user_id = :followedUserId";
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("userId", userId)
                .addValue("followedUserId", followedUserId);
        int affected = jdbcTemplate.update(sql, params);
        return affected > 0;
    }

    /**
     * 检查是否已关注
     */
    public boolean isFollowing(Long userId, Long followedUserId) {
        String sql = "SELECT COUNT(*) FROM user_follow_user WHERE user_id = :userId AND followed_user_id = :followedUserId";
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("userId", userId)
                .addValue("followedUserId", followedUserId);
        Integer count = jdbcTemplate.queryForObject(sql, params, Integer.class);
        return count != null && count > 0;
    }

    /**
     * 获取用户关注列表
     */
    public List<Map<String, Object>> getFollowingList(Long userId, int offset, int pageSize) {
        String sql = """
            SELECT u.user_id, u.nickname, u.avatar_url, u.bio
            FROM user_follow_user fu
            JOIN app_user u ON fu.followed_user_id = u.user_id
            WHERE fu.user_id = :userId
            ORDER BY fu.created_at DESC
            LIMIT :offset, :pageSize
            """;
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("userId", userId)
                .addValue("offset", offset)
                .addValue("pageSize", pageSize);

        return jdbcTemplate.queryForList(sql, params);
    }

    /**
     * 获取用户粉丝列表
     */
    public List<Map<String, Object>> getFollowerList(Long userId, int offset, int pageSize) {
        String sql = """
            SELECT u.user_id, u.nickname, u.avatar_url, u.bio
            FROM user_follow_user fu
            JOIN app_user u ON fu.user_id = u.user_id
            WHERE fu.followed_user_id = :userId
            ORDER BY fu.created_at DESC
            LIMIT :offset, :pageSize
            """;
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("userId", userId)
                .addValue("offset", offset)
                .addValue("pageSize", pageSize);

        return jdbcTemplate.queryForList(sql, params);
    }

    /**
     * 获取关注数量
     */
    public long getFollowingCount(Long userId) {
        String sql = "SELECT COUNT(*) FROM user_follow_user WHERE user_id = :userId";
        MapSqlParameterSource params = new MapSqlParameterSource("userId", userId);
        Long count = jdbcTemplate.queryForObject(sql, params, Long.class);
        return count != null ? count : 0;
    }

    /**
     * 获取粉丝数量
     */
    public long getFollowerCount(Long userId) {
        String sql = "SELECT COUNT(*) FROM user_follow_user WHERE followed_user_id = :userId";
        MapSqlParameterSource params = new MapSqlParameterSource("userId", userId);
        Long count = jdbcTemplate.queryForObject(sql, params, Long.class);
        return count != null ? count : 0;
    }
}