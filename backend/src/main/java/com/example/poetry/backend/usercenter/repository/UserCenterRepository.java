package com.example.poetry.backend.usercenter.repository;

import com.example.poetry.backend.usercenter.dto.FavoritePoemDto;
import com.example.poetry.backend.usercenter.dto.FavoritePostDto;
import com.example.poetry.backend.usercenter.dto.FollowedUserDto;
import java.util.List;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class UserCenterRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public UserCenterRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public int countFollowedUsers(Long userId) {
        String sql = "SELECT COUNT(*) FROM user_follow_user WHERE user_id = :userId";
        Integer count = jdbcTemplate.queryForObject(sql, new MapSqlParameterSource("userId", userId), Integer.class);
        return count == null ? 0 : count;
    }

    public List<FollowedUserDto> listFollowedUsers(Long userId, int limit, int offset) {
        String sql = """
            SELECT
                u.user_id AS userId,
                u.username,
                u.nickname,
                u.email,
                DATE_FORMAT(f.created_at, '%%Y-%%m-%%d %%H:%%i:%%s') AS followedAt
            FROM user_follow_user f
            JOIN app_user u ON u.user_id = f.followed_user_id
            WHERE f.user_id = :userId
            ORDER BY f.created_at DESC, f.follow_id DESC
            LIMIT :limit OFFSET :offset
            """;
        MapSqlParameterSource params = new MapSqlParameterSource()
            .addValue("userId", userId)
            .addValue("limit", limit)
            .addValue("offset", offset);
        return jdbcTemplate.query(
            sql,
            params,
            (rs, rowNum) -> new FollowedUserDto(
                rs.getLong("userId"),
                rs.getString("username"),
                rs.getString("nickname"),
                rs.getString("email"),
                rs.getString("followedAt")
            )
        );
    }

    public int unfollow(Long userId, Long followedUserId) {
        String sql = "DELETE FROM user_follow_user WHERE user_id = :userId AND followed_user_id = :followedUserId";
        MapSqlParameterSource params = new MapSqlParameterSource()
            .addValue("userId", userId)
            .addValue("followedUserId", followedUserId);
        return jdbcTemplate.update(sql, params);
    }

    public int countFavoritePoems(Long userId) {
        String sql = "SELECT COUNT(*) FROM user_favorite_work WHERE user_id = :userId";
        Integer count = jdbcTemplate.queryForObject(sql, new MapSqlParameterSource("userId", userId), Integer.class);
        return count == null ? 0 : count;
    }

    public List<FavoritePoemDto> listFavoritePoems(Long userId, int limit, int offset) {
        String sql = """
            SELECT
                w.work_id AS workId,
                w.title,
                w.author_name_cache AS authorName,
                w.dynasty_name AS dynastyName,
                LEFT(w.content_text, 120) AS contentPreview,
                DATE_FORMAT(f.created_at, '%%Y-%%m-%%d %%H:%%i:%%s') AS collectedAt
            FROM user_favorite_work f
            JOIN poetry_work w ON w.work_id = f.work_id
            WHERE f.user_id = :userId
            ORDER BY f.created_at DESC, f.favorite_id DESC
            LIMIT :limit OFFSET :offset
            """;
        MapSqlParameterSource params = new MapSqlParameterSource()
            .addValue("userId", userId)
            .addValue("limit", limit)
            .addValue("offset", offset);
        return jdbcTemplate.query(
            sql,
            params,
            (rs, rowNum) -> new FavoritePoemDto(
                rs.getLong("workId"),
                rs.getString("title"),
                rs.getString("authorName"),
                rs.getString("dynastyName"),
                rs.getString("contentPreview"),
                rs.getString("collectedAt")
            )
        );
    }

    public int unfavoritePoem(Long userId, Long workId) {
        String sql = "DELETE FROM user_favorite_work WHERE user_id = :userId AND work_id = :workId";
        MapSqlParameterSource params = new MapSqlParameterSource()
            .addValue("userId", userId)
            .addValue("workId", workId);
        return jdbcTemplate.update(sql, params);
    }

    public int countFavoritePosts(Long userId) {
        String sql = "SELECT COUNT(*) FROM community_post_collect WHERE user_id = :userId";
        Integer count = jdbcTemplate.queryForObject(sql, new MapSqlParameterSource("userId", userId), Integer.class);
        return count == null ? 0 : count;
    }

    public List<FavoritePostDto> listFavoritePosts(Long userId, int limit, int offset) {
        String sql = """
            SELECT
                p.post_id AS postId,
                p.title,
                LEFT(p.content_text, 160) AS contentPreview,
                p.topic_tag AS topicTag,
                p.collect_count AS collectCount,
                au.user_id AS authorUserId,
                au.nickname AS authorNickname,
                DATE_FORMAT(c.created_at, '%%Y-%%m-%%d %%H:%%i:%%s') AS collectedAt
            FROM community_post_collect c
            JOIN community_post p ON p.post_id = c.post_id
            JOIN app_user au ON au.user_id = p.user_id
            WHERE c.user_id = :userId
            ORDER BY c.created_at DESC, c.collect_id DESC
            LIMIT :limit OFFSET :offset
            """;
        MapSqlParameterSource params = new MapSqlParameterSource()
            .addValue("userId", userId)
            .addValue("limit", limit)
            .addValue("offset", offset);
        return jdbcTemplate.query(
            sql,
            params,
            (rs, rowNum) -> new FavoritePostDto(
                rs.getLong("postId"),
                rs.getString("title"),
                rs.getString("contentPreview"),
                rs.getString("topicTag"),
                rs.getInt("collectCount"),
                rs.getLong("authorUserId"),
                rs.getString("authorNickname"),
                rs.getString("collectedAt")
            )
        );
    }

    public int unfavoritePost(Long userId, Long postId) {
        String sql = "DELETE FROM community_post_collect WHERE user_id = :userId AND post_id = :postId";
        MapSqlParameterSource params = new MapSqlParameterSource()
            .addValue("userId", userId)
            .addValue("postId", postId);
        return jdbcTemplate.update(sql, params);
    }

    public int decrementPostCollectCount(Long postId) {
        String sql = """
            UPDATE community_post
            SET collect_count = CASE WHEN collect_count > 0 THEN collect_count - 1 ELSE 0 END
            WHERE post_id = :postId
            """;
        return jdbcTemplate.update(sql, new MapSqlParameterSource("postId", postId));
    }
}

