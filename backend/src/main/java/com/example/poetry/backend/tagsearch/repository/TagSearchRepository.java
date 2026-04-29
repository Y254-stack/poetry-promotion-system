package com.example.poetry.backend.tagsearch.repository;

import com.example.poetry.backend.tagsearch.dto.PoemDetailDto;
import java.util.List;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class TagSearchRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public TagSearchRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
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
}
