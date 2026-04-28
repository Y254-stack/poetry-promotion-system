package com.example.poetry.backend.learning.repository;

import com.example.poetry.backend.learning.dto.QuizModels;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public class QuizRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public QuizRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Map<String, Object> getRandomSentenceForQuiz() {
        String sql = """
            SELECT ws.work_id, ws.sentence_text, pw.title, pw.author_name_cache as author
            FROM work_sentence ws
            JOIN poetry_work pw ON ws.work_id = pw.work_id
            WHERE ws.char_count BETWEEN 5 AND 12
            ORDER BY RAND()
            LIMIT 1
            """;
        return jdbcTemplate.queryForMap(sql, new MapSqlParameterSource());
    }

    public List<String> getRandomDistractorChars(int count) {
        String sql = """
            SELECT SUBSTRING(sentence_text, 1, 1) as char_val
            FROM work_sentence
            ORDER BY RAND()
            LIMIT :count
            """;
        return jdbcTemplate.query(sql, new MapSqlParameterSource("count", count), (rs, rowNum) -> rs.getString("char_val"));
    }

    public void saveQuizRecord(QuizModels.QuizSubmitRequest request) {
        String sql = """
            INSERT INTO quiz_record (user_id, work_id, quiz_type, is_correct, duration_seconds, question_payload)
            VALUES (:userId, :workId, :quizType, :isCorrect, :duration, :payload)
            """;
        MapSqlParameterSource params = new MapSqlParameterSource()
            .addValue("userId", request.userId())
            .addValue("workId", request.workId())
            .addValue("quizType", request.quizType())
            .addValue("isCorrect", request.isCorrect() ? 1 : 0)
            .addValue("duration", request.durationSeconds())
            .addValue("payload", "{}");
        jdbcTemplate.update(sql, params);
    }
}
