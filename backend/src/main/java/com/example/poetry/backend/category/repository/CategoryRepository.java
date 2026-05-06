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
        String sql = "SELECT dynasty_id, dynasty_name FROM dynasty ORDER BY dynasty_name";
        return jdbcTemplate.query(sql, (rs, rowNum) ->
                new DynastyDto(
                        rs.getLong("dynasty_id"),
                        rs.getString("dynasty_name")
                )
        );
    }

    public List<AuthorDto> getAllAuthors() {
        String sql = "SELECT a.author_id, a.author_name, d.dynasty_name " +
                     "FROM author a " +
                     "LEFT JOIN dynasty d ON a.dynasty_id = d.dynasty_id " +
                     "ORDER BY a.author_name";
        return jdbcTemplate.query(sql, (rs, rowNum) ->
                new AuthorDto(
                        rs.getLong("author_id"),
                        rs.getString("author_name"),
                        rs.getString("dynasty_name")
                )
        );
    }
}
