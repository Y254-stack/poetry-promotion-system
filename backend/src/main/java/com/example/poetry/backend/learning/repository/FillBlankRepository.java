package com.example.poetry.backend.learning.repository;

import com.example.poetry.backend.learning.dto.QuizModels;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class FillBlankRepository {

    private static final Logger log = LoggerFactory.getLogger(FillBlankRepository.class);
    private final NamedParameterJdbcTemplate jdbcTemplate;

    public FillBlankRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<Map<String, Object>> getRandomSentenceForQuiz() {
        long start = System.currentTimeMillis();
        
        // 策略：先随机获取一个work_id，再从该work中随机获取句子
        // 这样避免在大表上做OFFSET操作
        
        // 优先获取有译文的作品
        String getRandomWorkWithTranslation = """
            SELECT work_id FROM poetry_work 
            WHERE translation_text IS NOT NULL AND translation_text != ''
            ORDER BY RAND() LIMIT 1
            """;
        
        try {
            Long randomWorkId = jdbcTemplate.queryForObject(getRandomWorkWithTranslation, new MapSqlParameterSource(), Long.class);
            
            if (randomWorkId != null) {
                log.debug("随机选中的作品ID: {}", randomWorkId);
                
                // 从该作品中随机获取一个符合条件的句子
                String getSentenceFromWork = """
                    SELECT ws.work_id, ws.sentence_id, ws.sentence_text, pw.title, pw.author_name_cache as author, pw.translation_text as translation
                    FROM work_sentence ws
                    INNER JOIN poetry_work pw ON ws.work_id = pw.work_id
                    WHERE ws.work_id = :workId AND ws.char_count BETWEEN 5 AND 30
                    ORDER BY RAND() LIMIT 1
                    """;
                
                try {
                    Map<String, Object> result = jdbcTemplate.queryForMap(getSentenceFromWork, new MapSqlParameterSource("workId", randomWorkId));
                    long duration = System.currentTimeMillis() - start;
                    log.debug("获取到有译文的句子，SQL查询耗时: {}ms", duration);
                    return Optional.of(result);
                } catch (EmptyResultDataAccessException e) {
                    log.warn("该作品没有符合条件的句子，尝试普通查询...");
                }
            }
        } catch (EmptyResultDataAccessException e) {
            log.warn("没有找到有译文的作品，使用普通查询...");
        }
        
        // 使用普通查询（无译文限制）
        String countSql = "SELECT COUNT(*) FROM work_sentence WHERE char_count BETWEEN 5 AND 30";
        Integer total = jdbcTemplate.queryForObject(countSql, new MapSqlParameterSource(), Integer.class);
        
        if (total == null || total == 0) {
            log.warn("work_sentence表中没有符合条件的句子，放宽条件...");
            countSql = "SELECT COUNT(*) FROM work_sentence WHERE char_count >= 2";
            total = jdbcTemplate.queryForObject(countSql, new MapSqlParameterSource(), Integer.class);
            if (total == null || total == 0) {
                log.error("work_sentence表为空！");
                return Optional.empty();
            }
        }
        
        log.debug("符合条件的句子总数: {}", total);
        
        // 使用高效的随机方式
        int randomOffset = (int) (Math.random() * total);
        
        String sql = """
            SELECT ws.work_id, ws.sentence_id, ws.sentence_text, pw.title, pw.author_name_cache as author, pw.translation_text as translation
            FROM work_sentence ws
            INNER JOIN poetry_work pw ON ws.work_id = pw.work_id
            WHERE ws.char_count BETWEEN 5 AND 30
            LIMIT :offset, 1
            """;
        
        try {
            Map<String, Object> result = jdbcTemplate.queryForMap(sql, new MapSqlParameterSource("offset", randomOffset));
            long duration = System.currentTimeMillis() - start;
            log.debug("SQL查询耗时: {}ms", duration);
            return Optional.of(result);
        } catch (EmptyResultDataAccessException e) {
            log.warn("随机抽取失败");
            return Optional.empty();
        }
    }

    public List<String> getRandomDistractorChars(int count) {
        String sql = "SELECT SUBSTRING(sentence_text, FLOOR(RAND() * CHAR_LENGTH(sentence_text)) + 1, 1) as char_val " +
                     "FROM work_sentence WHERE char_count >= 5 LIMIT 30";
        List<String> allChars = jdbcTemplate.query(sql, new MapSqlParameterSource(), (rs, rowNum) -> rs.getString("char_val"));
        
        // 过滤标点符号和空字符
        allChars.removeIf(c -> c == null || c.isEmpty() || c.matches("[，。？！；：、,.?!;:\\s]"));
        
        if (allChars.isEmpty()) {
            log.warn("无法获取干扰字符，使用默认值");
            return List.of("花", "月", "山", "水", "人");
        }
        
        java.util.Collections.shuffle(allChars);
        int size = Math.min(count, allChars.size());
        return allChars.subList(0, size);
    }

    public void saveQuizRecord(QuizModels.QuizSubmitRequest request) {
        String sql = """
            INSERT INTO quiz_record (user_id, work_id, sentence_id, quiz_type, difficulty_level, 
                                    question_payload, answer_payload, correct_payload, 
                                    is_correct, score, duration_seconds)
            VALUES (:userId, :workId, :sentenceId, :quizType, :difficulty, 
                    :questionPayload, :answerPayload, :correctPayload, 
                    :isCorrect, :score, :duration)
            """;
        MapSqlParameterSource params = new MapSqlParameterSource()
            .addValue("userId", request.userId())
            .addValue("workId", request.workId())
            .addValue("sentenceId", request.sentenceId())
            .addValue("quizType", request.quizType())
            .addValue("difficulty", 1)
            .addValue("questionPayload", request.questionPayload() != null ? request.questionPayload() : "{}")
            .addValue("answerPayload", request.answerPayload() != null ? request.answerPayload() : "{}")
            .addValue("correctPayload", request.correctPayload() != null ? request.correctPayload() : "{}")
            .addValue("isCorrect", request.isCorrect() ? 1 : 0)
            .addValue("score", request.isCorrect() ? 10.0 : 0.0)
            .addValue("duration", request.durationSeconds());
        jdbcTemplate.update(sql, params);
    }
}