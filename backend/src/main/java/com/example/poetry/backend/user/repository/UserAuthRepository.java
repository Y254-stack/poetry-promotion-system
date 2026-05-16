package com.example.poetry.backend.user.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
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
            SELECT user_id, username, password_hash, nickname, email, avatar_url,
                   login_locked_until, failed_login_count, failed_login_window_start
            FROM app_user
            WHERE username = :account OR email = :account
            LIMIT 1
            """;
        List<UserAccount> rows = jdbcTemplate.query(
            sql,
            new MapSqlParameterSource("account", account),
            this::mapRow
        );
        return rows.stream().findFirst();
    }

    public Optional<UserAccount> findByUserId(Long userId) {
        String sql = """
            SELECT user_id, username, password_hash, nickname, email, avatar_url,
                   login_locked_until, failed_login_count, failed_login_window_start
            FROM app_user
            WHERE user_id = :userId
            LIMIT 1
            """;
        List<UserAccount> rows = jdbcTemplate.query(
            sql,
            new MapSqlParameterSource("userId", userId),
            this::mapRow
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

    public void updateLoginSecurity(
        long userId,
        LocalDateTime loginLockedUntil,
        int failedLoginCount,
        LocalDateTime failedLoginWindowStart
    ) {
        String sql = """
            UPDATE app_user SET
                login_locked_until = :loginLockedUntil,
                failed_login_count = :failedLoginCount,
                failed_login_window_start = :failedLoginWindowStart
            WHERE user_id = :userId
            """;
        MapSqlParameterSource params = new MapSqlParameterSource()
            .addValue("userId", userId)
            .addValue("loginLockedUntil", loginLockedUntil)
            .addValue("failedLoginCount", failedLoginCount)
            .addValue("failedLoginWindowStart", failedLoginWindowStart);
        jdbcTemplate.update(sql, params);
    }

    public void updateNickname(Long userId, String nickname) {
        String sql = "UPDATE app_user SET nickname = :nickname WHERE user_id = :userId";
        MapSqlParameterSource params = new MapSqlParameterSource()
            .addValue("nickname", nickname)
            .addValue("userId", userId);
        jdbcTemplate.update(sql, params);
    }

    private UserAccount mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new UserAccount(
            rs.getLong("user_id"),
            rs.getString("username"),
            rs.getString("password_hash"),
            rs.getString("nickname"),
            rs.getString("email"),
            rs.getString("avatar_url"),
            toLocalDateTime(rs.getTimestamp("login_locked_until")),
            rs.getInt("failed_login_count"),
            toLocalDateTime(rs.getTimestamp("failed_login_window_start"))
        );
    }

    private static LocalDateTime toLocalDateTime(Timestamp ts) {
        return ts == null ? null : ts.toLocalDateTime();
    }
}
