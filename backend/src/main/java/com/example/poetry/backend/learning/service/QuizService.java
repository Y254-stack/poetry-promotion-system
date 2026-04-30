package com.example.poetry.backend.learning.service;

import com.example.poetry.backend.learning.dto.QuizModels;
import com.example.poetry.backend.learning.repository.QuizRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class QuizService {

    private static final Logger log = LoggerFactory.getLogger(QuizService.class);
    private final QuizRepository quizRepository;

    public QuizService(QuizRepository quizRepository) {
        this.quizRepository = quizRepository;
    }

    /**
     * 生成随机接龙题目列表
     */
    public List<QuizModels.QuizQuestion> generateQuizQuestions(int limit) {
        log.info("========== 开始生成随机接龙题目 ==========");
        long totalStart = System.currentTimeMillis();

        // 随机获取足够多的诗词（limit * 3，因为一首诗可能产生多个题目）
        List<Map<String, Object>> poems = quizRepository.getRandomPoemsForQuiz(limit * 3);
        log.info("从数据库获取到 {} 首诗词", poems.size());

        if (poems.isEmpty()) {
            log.error("数据库中没有诗词数据！请检查 poetry_work 表");
            return List.of();
        }

        List<QuizModels.QuizQuestion> allQuestions = new ArrayList<>();

        for (Map<String, Object> poem : poems) {
            allQuestions.addAll(parsePoemToQuestions(poem));
        }

        log.info("解析出 {} 道原始题目", allQuestions.size());

        // 去重
        allQuestions = removeDuplicates(allQuestions);
        log.info("去重后剩余 {} 道题目", allQuestions.size());

        // 随机打乱
        Collections.shuffle(allQuestions);

        // 取前 limit 条
        List<QuizModels.QuizQuestion> result = allQuestions.stream().limit(limit).toList();

        long totalEnd = System.currentTimeMillis();
        log.info("========== 题目生成完成 ==========");
        log.info("总耗时: {}ms, 生成题目数: {}", (totalEnd - totalStart), result.size());

        return result;
    }

    /**
     * 把整首诗解析成 (上句 -> 下句) 的接龙对
     */
    private List<QuizModels.QuizQuestion> parsePoemToQuestions(Map<String, Object> poem) {
        List<QuizModels.QuizQuestion> questions = new ArrayList<>();

        String id = String.valueOf(poem.get("id"));
        String title = (String) poem.get("title");
        String author = (String) poem.get("author");
        String content = (String) poem.get("content");

        if (content == null || content.isEmpty()) {
            log.debug("诗词《{}》内容为空，跳过", title);
            return questions;
        }

        log.debug("处理诗词《{}》，原始content长度: {}", title, content.length());

        // 清理 content：去掉 <br/> 标签，保留标点符号
        String cleanContent = content
                .replaceAll("<br/?>", "。")      // <br> 或 <br/> 替换成句号
                .replaceAll("&nbsp;", "")        // 去除 &nbsp;
                .replaceAll("\\s+", " ")         // 多个空格合并成一个
                .trim();

        // 按句号、感叹号、问号、分号、冒号分割句子
        String[] sentences = cleanContent.split("[。！？；：]");

        List<String> validSentences = new ArrayList<>();
        for (String s : sentences) {
            s = s.trim();
            // 过滤掉太短的句子（少于3个字符）和空字符串
            if (s.length() >= 3 && !s.isEmpty()) {
                validSentences.add(s);
            }
        }

        log.debug("《{}》解析出 {} 个有效句子", title, validSentences.size());

        // 生成接龙对：第i句 -> 第i+1句
        for (int i = 0; i < validSentences.size() - 1; i++) {
            String firstLine = validSentences.get(i);
            String nextLine = validSentences.get(i + 1);

            // 过滤太短的句子（少于3个字不太适合做接龙）
            if (firstLine.length() >= 3 && nextLine.length() >= 3) {
                long uniqueId = (long) (Math.abs(id.hashCode()) * 10000L + i);

                questions.add(new QuizModels.QuizQuestion(
                        uniqueId,
                        firstLine,
                        nextLine,
                        title,
                        author
                ));
            }
        }

        log.debug("从《{}》解析出 {} 道接龙题目", title, questions.size());
        return questions;
    }

    /**
     * 去除重复的接龙对
     */
    private List<QuizModels.QuizQuestion> removeDuplicates(List<QuizModels.QuizQuestion> questions) {
        Map<String, QuizModels.QuizQuestion> uniqueMap = new LinkedHashMap<>();
        for (QuizModels.QuizQuestion q : questions) {
            String key = q.firstLine() + "||" + q.correctAnswer();
            if (!uniqueMap.containsKey(key)) {
                uniqueMap.put(key, q);
            }
        }
        return new ArrayList<>(uniqueMap.values());
    }

    /**
     * 验证答案
     */
    public boolean validateAnswer(String userAnswer, String correctAnswer) {
        if (userAnswer == null || correctAnswer == null) {
            return false;
        }

        String normalizedUser = userAnswer
                .replaceAll("[，。？！；：、\\s]", "")
                .trim()
                .toLowerCase();
        String normalizedCorrect = correctAnswer
                .replaceAll("[，。？！；：、\\s]", "")
                .trim()
                .toLowerCase();

        return normalizedUser.equals(normalizedCorrect);
    }

    /**
     * 提交单题答案
     */
    public QuizModels.SubmitResponse submitResult(QuizModels.SubmitRequest request) {
        boolean isCorrect = validateAnswer(request.userAnswer(), request.correctAnswer());

        // 创建新的请求对象，包含正确性判断
        QuizModels.SubmitRequest validatedRequest = new QuizModels.SubmitRequest(
                request.userId(),
                request.questionId(),
                request.userAnswer(),
                request.correctAnswer(),
                request.quizType(),
                isCorrect,
                request.durationSeconds(),
                request.questionPayload(),
                request.answerPayload(),
                request.correctPayload()
        );

        quizRepository.saveQuizRecord(validatedRequest);

        String message = isCorrect ? "回答正确！" : "回答错误，再接再厉";
        return new QuizModels.SubmitResponse(isCorrect, message);
    }
}