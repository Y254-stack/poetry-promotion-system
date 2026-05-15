package com.example.poetry.backend.community.repository;

import com.example.poetry.backend.community.dto.PostDetailResponse;
import com.example.poetry.backend.community.dto.PostResponse;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class PostRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public PostRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 创建帖子
     */
    public long createPost(
        Long userId,
        String title,
        String contentText,
        String topicTag,
        Long relatedWorkId,
        Long relatedAuthorId
    ) {
        String sql = """
            INSERT INTO community_post (
                user_id, post_type, title, content_text, topic_tag,
                related_work_id, related_author_id, status, created_at, updated_at
            )
            VALUES (
                :userId, 'NORMAL', :title, :contentText, :topicTag,
                :relatedWorkId, :relatedAuthorId, 'ACTIVE', :now, :now
            )
            """;
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("userId", userId)
                .addValue("title", title)
                .addValue("contentText", contentText)
                .addValue("topicTag", topicTag != null ? topicTag : "")
                .addValue("relatedWorkId", relatedWorkId)
                .addValue("relatedAuthorId", relatedAuthorId)
                .addValue("now", LocalDateTime.now());

        jdbcTemplate.update(sql, params);

        // 获取刚插入的帖子ID
        String idSql = "SELECT LAST_INSERT_ID()";
        Long postId = jdbcTemplate.queryForObject(idSql, new MapSqlParameterSource(), Long.class);
        if (postId == null) {
            throw new IllegalStateException("Failed to get created post id");
        }
        return postId;
    }

    /**
     * 获取帖子列表（分页，按时间倒序）
     */
    public List<PostResponse> getPosts(int offset, int pageSize) {
        String sql = """
            SELECT p.post_id, p.user_id, p.title, p.content_text, p.topic_tag,
                   p.view_count, p.like_count, p.comment_count, p.collect_count, p.created_at,
                   COALESCE(u.nickname, u.username) as author
            FROM community_post p
            LEFT JOIN app_user u ON p.user_id = u.user_id
            WHERE p.status = 'ACTIVE'
            ORDER BY p.created_at DESC
            LIMIT :offset, :pageSize
            """;
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("offset", offset)
                .addValue("pageSize", pageSize);

        return jdbcTemplate.query(sql, params, (rs, rowNum) -> {
            String contentText = rs.getString("content_text");
            String preview = contentText != null && contentText.length() > 50
                    ? contentText.substring(0, 50) + "..."
                    : contentText;
            return new PostResponse(
                    rs.getLong("post_id"),
                    rs.getLong("user_id"),
                    rs.getString("author"),
                    rs.getString("title"),
                    preview,
                    rs.getString("topic_tag"),
                    rs.getInt("view_count"),
                    rs.getInt("like_count"),
                    rs.getInt("comment_count"),
                    rs.getInt("collect_count"),
                    rs.getTimestamp("created_at").toLocalDateTime()
            );
        });
    }

    /**
     * 获取帖子总数
     */
    public long getPostCount() {
        String sql = "SELECT COUNT(*) FROM community_post WHERE status = 'ACTIVE'";
        Long count = jdbcTemplate.queryForObject(sql, new MapSqlParameterSource(), Long.class);
        return count != null ? count : 0;
    }

    /**
     * 获取帖子详情
     */
    public Optional<PostDetailResponse> getPostDetail(Long postId) {
        String sql = """
            SELECT p.post_id, p.user_id, p.title, p.content_text, p.topic_tag,
                   p.view_count, p.like_count, p.comment_count, p.collect_count, p.created_at, p.updated_at,
                   COALESCE(u.nickname, u.username) as author
            FROM community_post p
            LEFT JOIN app_user u ON p.user_id = u.user_id
            WHERE p.post_id = :postId AND p.status = 'ACTIVE'
            """;
        MapSqlParameterSource params = new MapSqlParameterSource("postId", postId);

        return jdbcTemplate.query(sql, params, (rs, rowNum) -> new PostDetailResponse(
                rs.getLong("post_id"),
                rs.getLong("user_id"),
                rs.getString("author"),
                rs.getString("title"),
                rs.getString("content_text"),
                rs.getString("topic_tag"),
                rs.getInt("view_count"),
                rs.getInt("like_count"),
                rs.getInt("comment_count"),
                rs.getInt("collect_count"),
                rs.getTimestamp("created_at").toLocalDateTime(),
                rs.getTimestamp("updated_at").toLocalDateTime()
        )).stream().findFirst();
    }

    /**
     * 增加浏览量
     */
    public void incrementViewCount(Long postId) {
        String sql = "UPDATE community_post SET view_count = view_count + 1 WHERE post_id = :postId";
        jdbcTemplate.update(sql, new MapSqlParameterSource("postId", postId));
    }

    /**
     * 增加评论数
     */
    public void incrementCommentCount(Long postId) {
        String sql = "UPDATE community_post SET comment_count = comment_count + 1, updated_at = :now WHERE post_id = :postId";
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("postId", postId)
                .addValue("now", LocalDateTime.now());
        jdbcTemplate.update(sql, params);
    }

    /**
     * 减少评论数
     */
    public void decrementCommentCount(Long postId) {
        String sql = "UPDATE community_post SET comment_count = comment_count - 1, updated_at = :now WHERE post_id = :postId AND comment_count > 0";
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("postId", postId)
                .addValue("now", LocalDateTime.now());
        jdbcTemplate.update(sql, params);
    }

    /**
     * 增加点赞数
     */
    public void incrementLikeCount(Long postId) {
        String sql = "UPDATE community_post SET like_count = like_count + 1 WHERE post_id = :postId";
        jdbcTemplate.update(sql, new MapSqlParameterSource("postId", postId));
    }

    /**
     * 减少点赞数
     */
    public void decrementLikeCount(Long postId) {
        String sql = "UPDATE community_post SET like_count = like_count - 1 WHERE post_id = :postId AND like_count > 0";
        jdbcTemplate.update(sql, new MapSqlParameterSource("postId", postId));
    }

    /**
     * 增加收藏数
     */
    public void incrementCollectCount(Long postId) {
        String sql = "UPDATE community_post SET collect_count = collect_count + 1 WHERE post_id = :postId";
        jdbcTemplate.update(sql, new MapSqlParameterSource("postId", postId));
    }

    /**
     * 减少收藏数
     */
    public void decrementCollectCount(Long postId) {
        String sql = "UPDATE community_post SET collect_count = collect_count - 1 WHERE post_id = :postId AND collect_count > 0";
        jdbcTemplate.update(sql, new MapSqlParameterSource("postId", postId));
    }

    /**
     * 获取用户的帖子列表（分页）
     */
    public List<PostResponse> getUserPosts(Long userId, int offset, int pageSize) {
        String sql = """
        SELECT p.post_id, p.user_id, p.title, p.content_text, p.topic_tag,
               p.view_count, p.like_count, p.comment_count, p.collect_count, p.created_at,
               COALESCE(u.nickname, u.username) as author
        FROM community_post p
        LEFT JOIN app_user u ON p.user_id = u.user_id
        WHERE p.user_id = :userId AND p.status = 'ACTIVE'
        ORDER BY p.created_at DESC
        LIMIT :offset, :pageSize
        """;
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("userId", userId)
                .addValue("offset", offset)
                .addValue("pageSize", pageSize);

        return jdbcTemplate.query(sql, params, (rs, rowNum) -> {
            String contentText = rs.getString("content_text");
            String preview = contentText != null && contentText.length() > 50
                    ? contentText.substring(0, 50) + "..."
                    : contentText;
            return new PostResponse(
                    rs.getLong("post_id"),
                    rs.getLong("user_id"),
                    rs.getString("author"),
                    rs.getString("title"),
                    preview,
                    rs.getString("topic_tag"),
                    rs.getInt("view_count"),
                    rs.getInt("like_count"),
                    rs.getInt("comment_count"),
                    rs.getInt("collect_count"),
                    rs.getTimestamp("created_at").toLocalDateTime()
            );
        });
    }

    /**
     * 获取用户的帖子总数
     */
    public long getUserPostCount(Long userId) {
        String sql = "SELECT COUNT(*) FROM community_post WHERE user_id = :userId AND status = 'ACTIVE'";
        MapSqlParameterSource params = new MapSqlParameterSource("userId", userId);
        Long count = jdbcTemplate.queryForObject(sql, params, Long.class);
        return count != null ? count : 0;
    }

    /**
     * 物理删除帖子
     */
    public void deletePost(Long postId) {
        String sql = "DELETE FROM community_post WHERE post_id = :postId";
        MapSqlParameterSource params = new MapSqlParameterSource("postId", postId);
        jdbcTemplate.update(sql, params);
    }
}
