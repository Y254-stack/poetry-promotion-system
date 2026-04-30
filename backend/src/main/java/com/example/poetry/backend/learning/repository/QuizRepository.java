package com.example.poetry.backend.learning.repository;

import com.example.poetry.backend.learning.dto.QuizModels;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public class QuizRepository {

    private static final Logger log = LoggerFactory.getLogger(QuizRepository.class);
    private final NamedParameterJdbcTemplate jdbcTemplate;

    public QuizRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 随机获取诗词用于生成接龙题目
     */
    public List<Map<String, Object>> getRandomPoemsForQuiz(int limit) {
        long start = System.currentTimeMillis();

        // 先测试连接和表是否存在
        try {
            Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM poetry_work WHERE content_text IS NOT NULL AND content_text != ''", new MapSqlParameterSource(), Integer.class);
            log.info("poetry_work 表中共有 {} 条有内容的诗词数据", count);
        } catch (Exception e) {
            log.error("无法连接到 poetry_work 表: {}", e.getMessage());
            return List.of();
        }

        String sql = """
            SELECT work_id as id, title, author_name_cache as author, dynasty_name as dynasty, content_text as content
            FROM poetry_work
            WHERE content_text IS NOT NULL AND content_text != ''
            ORDER BY RAND()
            LIMIT :limit
            """;

        try {
            List<Map<String, Object>> results = jdbcTemplate.queryForList(sql, new MapSqlParameterSource("limit", limit));
            long duration = System.currentTimeMillis() - start;
            log.info("随机获取诗词完成，共 {} 条，耗时: {}ms", results.size(), duration);

            if (!results.isEmpty()) {
                String content = (String) results.get(0).get("content");
                log.info("样例content前100字符: {}", content != null && content.length() > 100 ? content.substring(0, 100) : content);
            }

            return results;
        } catch (Exception e) {
            log.error("随机获取诗词失败: {}", e.getMessage());
            log.error("SQL错误详情: ", e);
            return List.of();
        }
    }

    /**
     * 保存答题记录
     */
    public void saveQuizRecord(QuizModels.SubmitRequest request) {
        String sql = """
            INSERT INTO quiz_record (user_id, question_id, quiz_type, 
                                    question_payload, answer_payload, correct_payload, 
                                    is_correct, duration_seconds)
            VALUES (:userId, :questionId, :quizType, 
                    :questionPayload, :answerPayload, :correctPayload, 
                    :isCorrect, :duration)
            """;

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("userId", request.userId())
                .addValue("questionId", request.questionId())
                .addValue("quizType", request.quizType())
                .addValue("questionPayload", request.questionPayload() != null ? request.questionPayload() : "{}")
                .addValue("answerPayload", request.answerPayload() != null ? request.answerPayload() : "{}")
                .addValue("correctPayload", request.correctPayload() != null ? request.correctPayload() : "{}")
                .addValue("isCorrect", request.isCorrect() ? 1 : 0)
                .addValue("duration", request.durationSeconds());

        jdbcTemplate.update(sql, params);
    }
}