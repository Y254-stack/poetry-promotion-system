package com.example.poetry.backend.category.repository;

import com.example.poetry.backend.category.dto.AuthorDto;
import com.example.poetry.backend.category.dto.DynastyDto;
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
                     "FROM (SELECT DISTINCT dynasty_name FROM author WHERE dynasty_name IS NOT NULL) AS dynasties " +
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
}
