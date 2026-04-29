package com.example.poetry.backend.learning.repository;

import com.example.poetry.backend.learning.dto.Shiwen;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;


@Repository
public class ShiwenRepository {

    private static final Logger log = LoggerFactory.getLogger(ShiwenRepository.class);
    private final NamedParameterJdbcTemplate jdbcTemplate;

    public ShiwenRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 随机获取 N 条诗词（用于生成接龙题目）
     */
    public List<Map<String, Object>> getRandomPoems(int limit) {
        long start = System.currentTimeMillis();

        String sql = """
            SELECT id, title, author, dynasty, content
            FROM shiwen
            WHERE content IS NOT NULL AND content != ''
            ORDER BY RAND()
            LIMIT :limit
            """;

        try {
            List<Map<String, Object>> results = jdbcTemplate.queryForList(sql, new MapSqlParameterSource("limit", limit * 3));
            long duration = System.currentTimeMillis() - start;
            log.debug("随机获取诗词完成，共 {} 条，耗时: {}ms", results.size(), duration);
            return results;
        } catch (Exception e) {
            log.error("随机获取诗词失败: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * 获取所有诗词（用于生成完整题库）
     */
    public List<Map<String, Object>> getAllPoems() {
        long start = System.currentTimeMillis();

        String sql = """
            SELECT id, title, author, dynasty, content
            FROM shiwen
            WHERE content IS NOT NULL AND content != ''
            """;

        try {
            List<Map<String, Object>> results = jdbcTemplate.queryForList(sql, new MapSqlParameterSource());
            long duration = System.currentTimeMillis() - start;
            log.debug("获取所有诗词完成，共 {} 条，耗时: {}ms", results.size(), duration);
            return results;
        } catch (Exception e) {
            log.error("获取所有诗词失败: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * 根据朝代查询诗词
     */
    public List<Map<String, Object>> getPoemsByDynasty(String dynasty) {
        String sql = """
            SELECT id, title, author, dynasty, content
            FROM shiwen
            WHERE dynasty = :dynasty AND content IS NOT NULL AND content != ''
            """;

        try {
            return jdbcTemplate.queryForList(sql, new MapSqlParameterSource("dynasty", dynasty));
        } catch (Exception e) {
            log.error("根据朝代查询诗词失败: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * 根据作者查询诗词
     */
    public List<Map<String, Object>> getPoemsByAuthor(String author) {
        String sql = """
            SELECT id, title, author, dynasty, content
            FROM shiwen
            WHERE author = :author AND content IS NOT NULL AND content != ''
            """;

        try {
            return jdbcTemplate.queryForList(sql, new MapSqlParameterSource("author", author));
        } catch (Exception e) {
            log.error("根据作者查询诗词失败: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * 根据ID查询单首诗词
     */
    public Optional<Map<String, Object>> getPoemById(String id) {
        String sql = """
            SELECT id, title, author, dynasty, content
            FROM shiwen
            WHERE id = :id
            """;

        try {
            Map<String, Object> result = jdbcTemplate.queryForMap(sql, new MapSqlParameterSource("id", id));
            return Optional.of(result);
        } catch (EmptyResultDataAccessException e) {
            log.warn("未找到ID为 {} 的诗词", id);
            return Optional.empty();
        }
    }

    /**
     * 获取诗词总数
     */
    public int getPoemCount() {
        String sql = "SELECT COUNT(*) FROM shiwen WHERE content IS NOT NULL AND content != ''";
        try {
            Integer count = jdbcTemplate.queryForObject(sql, new MapSqlParameterSource(), Integer.class);
            return count != null ? count : 0;
        } catch (Exception e) {
            log.error("获取诗词总数失败: {}", e.getMessage());
            return 0;
        }
    }
}