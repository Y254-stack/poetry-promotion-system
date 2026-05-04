package com.example.poetry.backend.tagsearch.repository;

import com.example.poetry.backend.tagsearch.dto.PoemDetailDto;
import com.example.poetry.backend.tagsearch.dto.PoemSearchItemDto;
import com.example.poetry.backend.tagsearch.dto.TagDto;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class TagSearchRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public TagSearchRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<TagDto> findHotTags(int limit) {
        String sql = """
            SELECT
                t.tag_id AS tagId,
                t.tag_name AS tagName,
                t.tag_type AS tagType,
                COUNT(wt.work_id) AS workCount
            FROM tag t
            LEFT JOIN work_tag wt ON wt.tag_id = t.tag_id
            GROUP BY t.tag_id, t.tag_name, t.tag_type
            ORDER BY workCount DESC, t.tag_id ASC
            LIMIT :limit
            """;
        return jdbcTemplate.query(sql, new MapSqlParameterSource("limit", limit), tagRowMapper());
    }

    public int countByTags(List<Long> tagIds) {
        String sql = """
            SELECT COUNT(*) FROM (
                SELECT w.work_id
                FROM poetry_work w
                JOIN work_tag wt ON wt.work_id = w.work_id
                WHERE wt.tag_id IN (:tagIds)
                GROUP BY w.work_id
                HAVING COUNT(DISTINCT wt.tag_id) = :tagCount
            ) result_count
            """;
        MapSqlParameterSource params = new MapSqlParameterSource()
            .addValue("tagIds", tagIds)
            .addValue("tagCount", tagIds.size());
        Integer result = jdbcTemplate.queryForObject(sql, params, Integer.class);
        return result == null ? 0 : result;
    }

    public List<PoemSearchItemDto> searchByTags(List<Long> tagIds, String sort, int page, int pageSize) {
        String orderClause = switch (sort) {
            case "publish_time" -> "wm.publishTime DESC, wm.workId DESC";
            default -> "wm.hotScore DESC, wm.workId DESC";
        };

        String sql = """
            WITH matched_work AS (
                SELECT wt.work_id
                FROM work_tag wt
                WHERE wt.tag_id IN (:tagIds)
                GROUP BY wt.work_id
                HAVING COUNT(DISTINCT wt.tag_id) = :tagCount
            ),
            work_meta AS (
                SELECT
                    w.work_id AS workId,
                    w.title,
                    w.author_name_cache AS authorName,
                    w.dynasty_name AS dynastyName,
                    LEFT(w.content_text, 120) AS contentPreview,
                    ROUND(
                        (CASE WHEN w.translation_text IS NOT NULL AND w.translation_text <> '' THEN 30 ELSE 0 END) +
                        (CASE WHEN w.annotation_text IS NOT NULL AND w.annotation_text <> '' THEN 25 ELSE 0 END) +
                        (CASE WHEN w.appreciation_text IS NOT NULL AND w.appreciation_text <> '' THEN 20 ELSE 0 END) +
                        (CASE WHEN w.char_count BETWEEN 12 AND 220 THEN 10 ELSE 0 END),
                        2
                    ) AS hotScore,
                    DATE_FORMAT(w.created_at, '%%Y-%%m-%%d %%H:%%i:%%s') AS publishTime
                FROM poetry_work w
                JOIN matched_work mw ON mw.work_id = w.work_id
            )
            SELECT
                wm.workId,
                wm.title,
                wm.authorName,
                wm.dynastyName,
                wm.contentPreview,
                (
                    SELECT GROUP_CONCAT(t.tag_name ORDER BY t.tag_name SEPARATOR ',')
                    FROM work_tag wt2
                    JOIN tag t ON t.tag_id = wt2.tag_id
                    WHERE wt2.work_id = wm.workId
                      AND wt2.tag_id IN (:tagIds)
                ) AS matchedTags,
                wm.hotScore,
                wm.publishTime
            FROM work_meta wm
            ORDER BY %s
            LIMIT :limit OFFSET :offset
            """.formatted(orderClause);

        MapSqlParameterSource params = new MapSqlParameterSource()
            .addValue("tagIds", tagIds)
            .addValue("tagCount", tagIds.size())
            .addValue("limit", pageSize)
            .addValue("offset", Math.max(page - 1, 0) * pageSize);

        return jdbcTemplate.query(sql, params, poemSearchRowMapper());
    }

    public List<TagDto> findRecommendedTags(List<Long> excludedTagIds, int limit) {
        String sql = """
            SELECT
                t.tag_id AS tagId,
                t.tag_name AS tagName,
                t.tag_type AS tagType,
                COUNT(wt.work_id) AS workCount
            FROM tag t
            LEFT JOIN work_tag wt ON wt.tag_id = t.tag_id
            %s
            GROUP BY t.tag_id, t.tag_name, t.tag_type
            ORDER BY workCount DESC, t.tag_id ASC
            LIMIT :limit
            """.formatted(
            excludedTagIds.isEmpty() ? "" : "WHERE t.tag_id NOT IN (:excludedTagIds)"
        );

        MapSqlParameterSource params = new MapSqlParameterSource().addValue("limit", limit);
        if (!excludedTagIds.isEmpty()) {
            params.addValue("excludedTagIds", excludedTagIds);
        }
        return jdbcTemplate.query(sql, params, tagRowMapper());
    }

    public PoemDetailDto findPoemDetail(Long workId) {
        String sql = """
            SELECT
                work_id AS workId,
                title,
                author_name_cache AS authorName,
                dynasty_name AS dynastyName,
                content_text AS contentText,
                translation_text AS translationText,
                annotation_text AS annotationText,
                appreciation_text AS appreciationText
            FROM poetry_work
            WHERE work_id = :workId
            """;
        List<PoemDetailDto> result = jdbcTemplate.query(
            sql,
            new MapSqlParameterSource("workId", workId),
            (rs, rowNum) -> new PoemDetailDto(
                rs.getLong("workId"),
                rs.getString("title"),
                rs.getString("authorName"),
                rs.getString("dynastyName"),
                rs.getString("contentText"),
                rs.getString("translationText"),
                rs.getString("annotationText"),
                rs.getString("appreciationText")
            )
        );
        return result.isEmpty() ? null : result.get(0);
    }

    private RowMapper<TagDto> tagRowMapper() {
        return (rs, rowNum) -> new TagDto(
            rs.getLong("tagId"),
            rs.getString("tagName"),
            rs.getString("tagType"),
            rs.getInt("workCount")
        );
    }

    private RowMapper<PoemSearchItemDto> poemSearchRowMapper() {
        return (rs, rowNum) -> new PoemSearchItemDto(
            rs.getLong("workId"),
            rs.getString("title"),
            rs.getString("authorName"),
            rs.getString("dynastyName"),
            rs.getString("contentPreview"),
            rs.getString("matchedTags"),
            rs.getInt("hotScore"),
            rs.getString("publishTime")
        );
    }

    private List<String> splitTags(ResultSet rs) throws SQLException {
        String raw = rs.getString("matchedTags");
        if (raw == null || raw.isBlank()) {
            return Collections.emptyList();
        }
        return Arrays.stream(raw.split(","))
            .map(String::trim)
            .filter(value -> !value.isBlank())
            .toList();
    }

    public int countByTitle(String query) {
        String sql = """
            SELECT COUNT(*)
            FROM poetry_work
            WHERE title LIKE CONCAT('%', :query, '%')
            """;
        MapSqlParameterSource params = new MapSqlParameterSource("query", query);
        Integer result = jdbcTemplate.queryForObject(sql, params, Integer.class);
        return result == null ? 0 : result;
    }

    public List<PoemSearchItemDto> searchByTitle(String query, int page, int pageSize) {
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
                DATE_FORMAT(w.created_at, '%%Y-%%m-%%d %%H:%%i:%%s') AS publishTime
            FROM poetry_work w
            WHERE w.title LIKE CONCAT('%', :query, '%')
            ORDER BY w.work_id DESC
            LIMIT :limit OFFSET :offset
            """;

        MapSqlParameterSource params = new MapSqlParameterSource()
            .addValue("query", query)
            .addValue("limit", pageSize)
            .addValue("offset", Math.max(page - 1, 0) * pageSize);

        return jdbcTemplate.query(sql, params, poemSearchRowMapper());
    }

    public int countByAuthor(String query) {
        String sql = """
            SELECT COUNT(*)
            FROM poetry_work
            WHERE author_name_cache LIKE CONCAT('%', :query, '%')
            """;
        MapSqlParameterSource params = new MapSqlParameterSource("query", query);
        Integer result = jdbcTemplate.queryForObject(sql, params, Integer.class);
        return result == null ? 0 : result;
    }

    public List<PoemSearchItemDto> searchByAuthor(String query, int page, int pageSize) {
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
                DATE_FORMAT(w.created_at, '%%Y-%%m-%%d %%H:%%i:%%s') AS publishTime
            FROM poetry_work w
            WHERE w.author_name_cache LIKE CONCAT('%', :query, '%')
            ORDER BY w.work_id DESC
            LIMIT :limit OFFSET :offset
            """;

        MapSqlParameterSource params = new MapSqlParameterSource()
            .addValue("query", query)
            .addValue("limit", pageSize)
            .addValue("offset", Math.max(page - 1, 0) * pageSize);

        return jdbcTemplate.query(sql, params, poemSearchRowMapper());
    }

    public int countByTitleOrAuthor(String query) {
        String sql = """
            SELECT COUNT(*)
            FROM poetry_work
            WHERE title LIKE CONCAT('%', :query, '%')
               OR author_name_cache LIKE CONCAT('%', :query, '%')
            """;
        MapSqlParameterSource params = new MapSqlParameterSource("query", query);
        Integer result = jdbcTemplate.queryForObject(sql, params, Integer.class);
        return result == null ? 0 : result;
    }

    public List<PoemSearchItemDto> searchByTitleOrAuthor(String query, int page, int pageSize) {
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
                DATE_FORMAT(w.created_at, '%%Y-%%m-%%d %%H:%%i:%%s') AS publishTime
            FROM poetry_work w
            WHERE w.title LIKE CONCAT('%', :query, '%')
               OR w.author_name_cache LIKE CONCAT('%', :query, '%')
            ORDER BY w.work_id DESC
            LIMIT :limit OFFSET :offset
            """;

        MapSqlParameterSource params = new MapSqlParameterSource()
            .addValue("query", query)
            .addValue("limit", pageSize)
            .addValue("offset", Math.max(page - 1, 0) * pageSize);

        return jdbcTemplate.query(sql, params, poemSearchRowMapper());
    }
}

