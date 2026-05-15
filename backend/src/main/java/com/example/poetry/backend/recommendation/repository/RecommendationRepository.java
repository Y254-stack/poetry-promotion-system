package com.example.poetry.backend.recommendation.repository;

import com.example.poetry.backend.recommendation.dto.DailyRecommendationItemDto;
import com.example.poetry.backend.recommendation.dto.DailyRecommendationMetaDto;
import com.example.poetry.backend.tagsearch.dto.PoemSearchItemDto;
import java.time.LocalDate;
import java.util.List;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class RecommendationRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public RecommendationRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<LocalDate> findRecommendationDates() {
        String sql = """
            SELECT recommend_date
            FROM daily_recommendation
            ORDER BY recommend_date ASC
            """;
        return jdbcTemplate.query(
            sql,
            (rs, rowNum) -> rs.getDate("recommend_date").toLocalDate()
        );
    }

    public DailyRecommendationMetaDto findDailyRecommendationMeta(LocalDate recommendDate) {
        String sql = """
            SELECT recommend_date, theme_name, intro_text
            FROM daily_recommendation
            WHERE recommend_date = :recommendDate
            LIMIT 1
            """;
        List<DailyRecommendationMetaDto> rows = jdbcTemplate.query(
            sql,
            new MapSqlParameterSource("recommendDate", recommendDate),
            (rs, rowNum) -> new DailyRecommendationMetaDto(
                rs.getDate("recommend_date").toLocalDate().toString(),
                rs.getString("theme_name"),
                rs.getString("intro_text")
            )
        );
        return rows.isEmpty() ? null : rows.get(0);
    }

    public List<DailyRecommendationItemDto> findDailyRecommendationItems(LocalDate recommendDate, int limit) {
        String sql = """
            SELECT
                w.work_id AS workId,
                w.author_id AS authorId,
                w.title,
                w.author_name_cache AS authorName,
                w.dynasty_name AS dynastyName,
                i.reason_type AS reasonType,
                i.reason_text AS reasonText
            FROM daily_recommendation r
            JOIN daily_recommend_item i
                ON i.recommendation_id = r.recommendation_id
            JOIN poetry_work w
                ON w.work_id = i.work_id
            WHERE r.recommend_date = :recommendDate
            ORDER BY i.sort_no ASC
            LIMIT :limit
            """;
        MapSqlParameterSource params = new MapSqlParameterSource()
            .addValue("recommendDate", recommendDate)
            .addValue("limit", Math.max(limit, 1));

        return jdbcTemplate.query(
            sql,
            params,
            (rs, rowNum) -> new DailyRecommendationItemDto(
                rs.getLong("workId"),
                rs.getObject("authorId", Long.class),
                rs.getString("title"),
                rs.getString("authorName"),
                rs.getString("dynastyName"),
                rs.getString("reasonType"),
                rs.getString("reasonText")
            )
        );
    }

    public List<PoemSearchItemDto> findRelatedWorks(Long workId, int limit) {
        String sql = """
            SELECT
                r.related_work_id AS workId,
                w.title,
                w.author_name_cache AS authorName,
                w.dynasty_name AS dynastyName,
                LEFT(COALESCE(w.content_text, ''), 120) AS contentPreview,
                '' AS matchedTags,
                ROUND(r.score, 0) AS hotScore,
                NULL AS publishTime
            FROM work_relation r
            JOIN poetry_work w
                ON w.work_id = r.related_work_id
            WHERE r.work_id = :workId
            ORDER BY r.score DESC, r.related_work_id DESC
            LIMIT :limit
            """;
        MapSqlParameterSource params = new MapSqlParameterSource()
            .addValue("workId", workId)
            .addValue("limit", Math.max(limit, 1));

        return jdbcTemplate.query(
            sql,
            params,
            (rs, rowNum) -> new PoemSearchItemDto(
                rs.getLong("workId"),
                rs.getString("title"),
                rs.getString("authorName"),
                rs.getString("dynastyName"),
                rs.getString("contentPreview"),
                rs.getString("matchedTags"),
                rs.getInt("hotScore"),
                rs.getString("publishTime")
            )
        );
    }
}
