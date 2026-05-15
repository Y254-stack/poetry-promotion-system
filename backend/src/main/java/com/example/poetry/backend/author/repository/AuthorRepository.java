package com.example.poetry.backend.author.repository;

import com.example.poetry.backend.author.dto.AuthorDetailDto;
import com.example.poetry.backend.tagsearch.dto.PoemSearchItemDto;
import java.util.List;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class AuthorRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public AuthorRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public AuthorDetailDto findAuthorDetail(Long authorId, int workLimit) {
        String detailSql = """
            SELECT
                a.author_id AS authorId,
                a.canonical_name AS authorName,
                a.dynasty_name AS dynastyName,
                a.intro_text AS introText,
                COUNT(w.work_id) AS workCount
            FROM author a
            LEFT JOIN poetry_work w ON w.author_id = a.author_id
            WHERE a.author_id = :authorId
            GROUP BY a.author_id, a.canonical_name, a.dynasty_name, a.intro_text
            """;

        List<AuthorDetailDto> details = jdbcTemplate.query(
            detailSql,
            new MapSqlParameterSource("authorId", authorId),
            (rs, rowNum) -> new AuthorDetailDto(
                rs.getLong("authorId"),
                rs.getString("authorName"),
                rs.getString("dynastyName"),
                rs.getString("introText"),
                rs.getInt("workCount"),
                List.of()
            )
        );

        if (details.isEmpty()) {
            return null;
        }

        List<PoemSearchItemDto> works = findRepresentativeWorks(authorId, workLimit);
        AuthorDetailDto detail = details.get(0);
        return new AuthorDetailDto(
            detail.authorId(),
            detail.authorName(),
            detail.dynastyName(),
            detail.introText(),
            detail.workCount(),
            works
        );
    }

    private List<PoemSearchItemDto> findRepresentativeWorks(Long authorId, int workLimit) {
        String worksSql = """
            SELECT
                w.work_id AS workId,
                w.title,
                w.author_name_cache AS authorName,
                w.dynasty_name AS dynastyName,
                LEFT(COALESCE(w.content_text, ''), 120) AS contentPreview,
                NULL AS matchedTags,
                ROUND(
                    (CASE WHEN w.translation_text IS NOT NULL AND w.translation_text <> '' THEN 30 ELSE 0 END) +
                    (CASE WHEN w.annotation_text IS NOT NULL AND w.annotation_text <> '' THEN 25 ELSE 0 END) +
                    (CASE WHEN w.appreciation_text IS NOT NULL AND w.appreciation_text <> '' THEN 20 ELSE 0 END) +
                    (CASE WHEN w.char_count BETWEEN 12 AND 220 THEN 10 ELSE 0 END),
                    0
                ) AS hotScore,
                DATE_FORMAT(w.created_at, '%Y-%m-%d %H:%i:%s') AS publishTime
            FROM poetry_work w
            WHERE w.author_id = :authorId
            ORDER BY hotScore DESC, w.char_count DESC, w.work_id DESC
            LIMIT :limit
            """;

        MapSqlParameterSource params = new MapSqlParameterSource()
            .addValue("authorId", authorId)
            .addValue("limit", Math.max(workLimit, 1));

        return jdbcTemplate.query(
            worksSql,
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
