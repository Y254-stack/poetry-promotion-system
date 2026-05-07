package com.example.poetry.backend.category.repository;

import com.example.poetry.backend.category.dto.AuthorDto;
import com.example.poetry.backend.category.dto.DynastyDto;
import com.example.poetry.backend.tagsearch.dto.PoemSearchItemDto;
import com.example.poetry.backend.tagsearch.dto.PoemTitleSearchResponse;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
public class CategoryRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public CategoryRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<DynastyDto> getAllDynasties() {
        String sql = "SELECT ROW_NUMBER() OVER (ORDER BY dynasty_name) as dynasty_id, dynasty_name " +
                     "FROM (SELECT DISTINCT dynasty_name FROM author WHERE dynasty_name IS NOT NULL AND dynasty_name NOT IN ('元代', '两汉')) AS dynasties " +
                     "ORDER BY dynasty_name";
        return jdbcTemplate.query(sql, (rs, rowNum) ->
                new DynastyDto(
                        rs.getLong("dynasty_id"),
                        rs.getString("dynasty_name")
                )
        );
    }

    public List<AuthorDto> getAllAuthors() {
        String sql = "SELECT a.author_id, a.canonical_name as author_name, a.dynasty_name " +
                     "FROM author a " +
                     "WHERE a.canonical_name IS NOT NULL " +
                     "ORDER BY a.canonical_name";
        return jdbcTemplate.query(sql, (rs, rowNum) ->
                new AuthorDto(
                        rs.getLong("author_id"),
                        rs.getString("author_name"),
                        rs.getString("dynasty_name")
                )
        );
    }

    public PoemTitleSearchResponse getPoemsByDynasty(String dynastyName, int page, int pageSize) {
        int offset = (page - 1) * pageSize;

        String countSql = "SELECT COUNT(*) FROM v_poetry_work_overview WHERE dynasty_name = :dynastyName";
        Map<String, Object> countParams = new HashMap<>();
        countParams.put("dynastyName", dynastyName);
        Integer total = jdbcTemplate.queryForObject(countSql, countParams, Integer.class);

        String sql = "SELECT work_id, title, author_name, dynasty_name, content_preview " +
                     "FROM v_poetry_work_overview " +
                     "WHERE dynasty_name = :dynastyName " +
                     "ORDER BY work_id " +
                     "LIMIT :limit OFFSET :offset";
        Map<String, Object> params = new HashMap<>();
        params.put("dynastyName", dynastyName);
        params.put("limit", pageSize);
        params.put("offset", offset);

        List<PoemSearchItemDto> items = jdbcTemplate.query(sql, params, (rs, rowNum) ->
                new PoemSearchItemDto(
                        rs.getLong("work_id"),
                        rs.getString("title"),
                        rs.getString("author_name"),
                        rs.getString("dynasty_name"),
                        rs.getString("content_preview"),
                        null, // matchedTags
                        0,    // hotScore
                        null  // publishTime
                )
        );

        return new PoemTitleSearchResponse(
                dynastyName,
                page,
                pageSize,
                total != null ? total : 0,
                items,
                null
        );
    }

    public PoemTitleSearchResponse getPoemsByAuthorId(Long authorId, int page, int pageSize) {
        int offset = (page - 1) * pageSize;

        String countSql = "SELECT COUNT(*) FROM poetry_work w " +
                         "JOIN author a ON w.author_name_cache = a.canonical_name " +
                         "WHERE a.author_id = :authorId";
        Map<String, Object> countParams = new HashMap<>();
        countParams.put("authorId", authorId);
        Integer total = jdbcTemplate.queryForObject(countSql, countParams, Integer.class);

        String sql = "SELECT w.work_id, w.title, w.author_name_cache as author_name, w.dynasty_name, " +
                     "LEFT(w.content_text, 120) as content_preview " +
                     "FROM poetry_work w " +
                     "JOIN author a ON w.author_name_cache = a.canonical_name " +
                     "WHERE a.author_id = :authorId " +
                     "ORDER BY w.work_id " +
                     "LIMIT :limit OFFSET :offset";
        Map<String, Object> params = new HashMap<>();
        params.put("authorId", authorId);
        params.put("limit", pageSize);
        params.put("offset", offset);

        List<PoemSearchItemDto> items = jdbcTemplate.query(sql, params, (rs, rowNum) ->
                new PoemSearchItemDto(
                        rs.getLong("work_id"),
                        rs.getString("title"),
                        rs.getString("author_name"),
                        rs.getString("dynasty_name"),
                        rs.getString("content_preview"),
                        null, // matchedTags
                        0,    // hotScore
                        null  // publishTime
                )
        );

        return new PoemTitleSearchResponse(
                String.valueOf(authorId),
                page,
                pageSize,
                total != null ? total : 0,
                items,
                null
        );
    }
}
