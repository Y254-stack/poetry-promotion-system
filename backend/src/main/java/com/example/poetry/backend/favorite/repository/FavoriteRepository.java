package com.example.poetry.backend.favorite.repository;

import com.example.poetry.backend.tagsearch.dto.PoemSearchItemDto;
import java.util.List;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class FavoriteRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public FavoriteRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public boolean isFavorited(Long userId, Long workId) {
        String sql = """
            SELECT COUNT(*) FROM user_favorite_poem
            WHERE user_id = :userId AND work_id = :workId
            """;
        MapSqlParameterSource params = new MapSqlParameterSource()
            .addValue("userId", userId)
            .addValue("workId", workId);
        Integer count = jdbcTemplate.queryForObject(sql, params, Integer.class);
        return count != null && count > 0;
    }

    public int addFavorite(Long userId, Long workId) {
        String sql = """
            INSERT INTO user_favorite_poem (user_id, work_id)
            VALUES (:userId, :workId)
            """;
        MapSqlParameterSource params = new MapSqlParameterSource()
            .addValue("userId", userId)
            .addValue("workId", workId);
        return jdbcTemplate.update(sql, params);
    }

    public int removeFavorite(Long userId, Long workId) {
        String sql = """
            DELETE FROM user_favorite_poem
            WHERE user_id = :userId AND work_id = :workId
            """;
        MapSqlParameterSource params = new MapSqlParameterSource()
            .addValue("userId", userId)
            .addValue("workId", workId);
        return jdbcTemplate.update(sql, params);
    }

    public int countFavorites(Long userId) {
        String sql = """
            SELECT COUNT(*) FROM user_favorite_poem
            WHERE user_id = :userId
            """;
        MapSqlParameterSource params = new MapSqlParameterSource("userId", userId);
        Integer count = jdbcTemplate.queryForObject(sql, params, Integer.class);
        return count == null ? 0 : count;
    }

    public List<PoemSearchItemDto> getFavoriteList(Long userId, int page, int pageSize) {
        String sql = """
            SELECT
                w.work_id AS workId,
                w.title,
                w.author_name_cache AS authorName,
                w.dynasty_name AS dynastyName,
                LEFT(w.content_text, 120) AS contentPreview,
                '' AS matchedTags,
                ROUND(
                    (CASE WHEN w.translation_text IS NOT NULL AND w.translation_text <> '' THEN 30 ELSE 0 END) +
                    (CASE WHEN w.annotation_text IS NOT NULL AND w.annotation_text <> '' THEN 25 ELSE 0 END) +
                    (CASE WHEN w.appreciation_text IS NOT NULL AND w.appreciation_text <> '' THEN 20 ELSE 0 END) +
                    (CASE WHEN w.char_count BETWEEN 12 AND 220 THEN 10 ELSE 0 END),
                    2
                ) AS hotScore,
                DATE_FORMAT(ufp.created_at, '%Y-%m-%d %H:%i:%s') AS publishTime
            FROM user_favorite_poem ufp
            JOIN poetry_work w ON w.work_id = ufp.work_id
            WHERE ufp.user_id = :userId
            ORDER BY ufp.created_at DESC
            LIMIT :limit OFFSET :offset
            """;

        MapSqlParameterSource params = new MapSqlParameterSource()
            .addValue("userId", userId)
            .addValue("limit", pageSize)
            .addValue("offset", Math.max(page - 1, 0) * pageSize);

        return jdbcTemplate.query(sql, params, (rs, rowNum) -> new PoemSearchItemDto(
            rs.getLong("workId"),
            rs.getString("title"),
            rs.getString("authorName"),
            rs.getString("dynastyName"),
            rs.getString("contentPreview"),
            rs.getString("matchedTags"),
            rs.getInt("hotScore"),
            rs.getString("publishTime")
        ));
    }
}
