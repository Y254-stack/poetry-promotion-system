package com.example.poetry.backend.user.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class UserAuthRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public UserAuthRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public boolean existsByUsername(String username) {
        String sql = "SELECT COUNT(*) FROM app_user WHERE username = :username";
        Integer count = jdbcTemplate.queryForObject(
            sql,
            new MapSqlParameterSource("username", username),
            Integer.class
        );
        return count != null && count > 0;
    }

    public boolean existsByEmail(String email) {
        String sql = "SELECT COUNT(*) FROM app_user WHERE email = :email";
        Integer count = jdbcTemplate.queryForObject(
            sql,
            new MapSqlParameterSource("email", email),
            Integer.class
        );
        return count != null && count > 0;
    }

    public long createUser(String username, String passwordHash, String nickname, String email) {
        String sql = """
            INSERT INTO app_user(username, password_hash, nickname, email)
            VALUES (:username, :passwordHash, :nickname, :email)
            """;
        MapSqlParameterSource params = new MapSqlParameterSource()
            .addValue("username", username)
            .addValue("passwordHash", passwordHash)
            .addValue("nickname", nickname)
            .addValue("email", email);
        jdbcTemplate.update(sql, params);

        String idSql = "SELECT user_id FROM app_user WHERE username = :username";
        Long userId = jdbcTemplate.queryForObject(
            idSql,
            new MapSqlParameterSource("username", username),
            Long.class
        );
        if (userId == null) {
            throw new IllegalStateException("Failed to resolve created user id");
        }
        return userId;
    }

    public Optional<UserAccount> findByAccount(String account) {
        String sql = """
            SELECT user_id, username, password_hash, nickname, email
            FROM app_user
            WHERE username = :account OR email = :account
            LIMIT 1
            """;
        List<UserAccount> rows = jdbcTemplate.query(
            sql,
            new MapSqlParameterSource("account", account),
            (rs, rowNum) -> new UserAccount(
                rs.getLong("user_id"),
                rs.getString("username"),
                rs.getString("password_hash"),
                rs.getString("nickname"),
                rs.getString("email")
            )
        );
        return rows.stream().findFirst();
    }

    public Optional<UserAccount> findByUserId(Long userId) {
        String sql = """
            SELECT user_id, username, password_hash, nickname, email
            FROM app_user
            WHERE user_id = :userId
            LIMIT 1
            """;
        List<UserAccount> rows = jdbcTemplate.query(
            sql,
            new MapSqlParameterSource("userId", userId),
            (rs, rowNum) -> new UserAccount(
                rs.getLong("user_id"),
                rs.getString("username"),
                rs.getString("password_hash"),
                rs.getString("nickname"),
                rs.getString("email")
            )
        );
        return rows.stream().findFirst();
    }

    public void updatePassword(Long userId, String newPasswordHash) {
        String sql = "UPDATE app_user SET password_hash = :passwordHash WHERE user_id = :userId";
        MapSqlParameterSource params = new MapSqlParameterSource()
            .addValue("passwordHash", newPasswordHash)
            .addValue("userId", userId);
        jdbcTemplate.update(sql, params);
    }
}
