package com.example.poetry.backend.favorite.repository;

import com.example.poetry.backend.favorite.dto.PostCollectListItemDto;
import java.util.List;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class PostCollectRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public PostCollectRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public int countCollects(Long userId, String searchQuery) {
        boolean hasQuery = searchQuery != null && !searchQuery.isBlank();
        String sql = """
            SELECT COUNT(*) FROM community_post_collect cpc
            JOIN community_post p ON p.post_id = cpc.post_id
            JOIN app_user u ON u.user_id = p.user_id
            WHERE cpc.user_id = :userId
              AND p.status = 'published'
            """ + collectSearchFilter(hasQuery);

        MapSqlParameterSource params = new MapSqlParameterSource("userId", userId);
        if (hasQuery) {
            params.addValue("likePattern", buildLikePattern(searchQuery.trim()));
        }
        Integer count = jdbcTemplate.queryForObject(sql, params, Integer.class);
        return count == null ? 0 : count;
    }

    public int removeCollect(Long userId, Long postId) {
        String sql = """
            DELETE FROM community_post_collect
            WHERE user_id = :userId AND post_id = :postId
            """;
        MapSqlParameterSource params = new MapSqlParameterSource()
            .addValue("userId", userId)
            .addValue("postId", postId);
        return jdbcTemplate.update(sql, params);
    }

    public List<PostCollectListItemDto> getCollectList(Long userId, int page, int pageSize, String searchQuery) {
        boolean hasQuery = searchQuery != null && !searchQuery.isBlank();
        String sql = """
            SELECT
                p.post_id AS postId,
                p.title,
                p.topic_tag AS topicTag,
                LEFT(COALESCE(p.content_text, ''), 120) AS contentPreview,
                u.nickname AS authorNickname,
                DATE_FORMAT(cpc.created_at, '%Y-%m-%d %H:%i:%s') AS collectedAt
            FROM community_post_collect cpc
            JOIN community_post p ON p.post_id = cpc.post_id
            JOIN app_user u ON u.user_id = p.user_id
            WHERE cpc.user_id = :userId
              AND p.status = 'published'
            """ + collectSearchFilter(hasQuery) + """
            ORDER BY cpc.created_at DESC
            LIMIT :limit OFFSET :offset
            """;

        MapSqlParameterSource params = new MapSqlParameterSource()
            .addValue("userId", userId)
            .addValue("limit", pageSize)
            .addValue("offset", Math.max(page - 1, 0) * pageSize);
        if (hasQuery) {
            params.addValue("likePattern", buildLikePattern(searchQuery.trim()));
        }

        return jdbcTemplate.query(sql, params, (rs, rowNum) -> new PostCollectListItemDto(
            rs.getLong("postId"),
            rs.getString("title"),
            rs.getString("topicTag"),
            rs.getString("contentPreview"),
            rs.getString("authorNickname"),
            rs.getString("collectedAt")
        ));
    }

    private static String collectSearchFilter(boolean hasQuery) {
        if (!hasQuery) {
            return "";
        }
        return """
             AND (
                COALESCE(p.title, '') LIKE :likePattern ESCAPE '\\\\' OR
                COALESCE(p.content_text, '') LIKE :likePattern ESCAPE '\\\\' OR
                COALESCE(p.topic_tag, '') LIKE :likePattern ESCAPE '\\\\' OR
                COALESCE(u.nickname, '') LIKE :likePattern ESCAPE '\\\\'
            )
            """;
    }

    private static String buildLikePattern(String raw) {
        StringBuilder escaped = new StringBuilder();
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if (c == '\\' || c == '%' || c == '_') {
                escaped.append('\\');
            }
            escaped.append(c);
        }
        return "%" + escaped + "%";
    }
}
