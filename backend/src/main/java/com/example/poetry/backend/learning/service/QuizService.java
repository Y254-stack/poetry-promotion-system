package com.example.poetry.backend.learning.service;

import com.example.poetry.backend.learning.dto.QuizQuestionDto;
import com.example.poetry.backend.learning.dto.Shiwen;
import com.example.poetry.backend.learning.repository.ShiwenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Pattern;

@Service
public class QuizService {

    private static final Logger log = LoggerFactory.getLogger(QuizService.class);
    private final ShiwenRepository shiwenRepository;

    public QuizService(ShiwenRepository shiwenRepository) {
        this.shiwenRepository = shiwenRepository;
    }

    /**
     * 随机获取 N 道接龙题目
     */
    public List<QuizQuestionDto> getRandomQuizQuestions(int limit) {
        long start = System.currentTimeMillis();

        // 随机取足够多的诗词（limit * 3，因为一首诗可能产生多个题目）
        List<Map<String, Object>> randomPoems = shiwenRepository.getRandomPoems(limit * 3);
        List<QuizQuestionDto> allQuestions = new ArrayList<>();

        for (Map<String, Object> poem : randomPoems) {
            allQuestions.addAll(parsePoemToQuestions(poem));
        }

        allQuestions = removeDuplicates(allQuestions);
        Collections.shuffle(allQuestions);

        List<QuizQuestionDto> result = allQuestions.stream().limit(limit).toList();

        long duration = System.currentTimeMillis() - start;
        log.debug("生成 {} 道接龙题目，耗时: {}ms", result.size(), duration);

        return result;
    }

    /**
     * 获取所有接龙题目
     */
    public List<QuizQuestionDto> getAllQuizQuestions() {
        List<Map<String, Object>> allPoems = shiwenRepository.getAllPoems();
        List<QuizQuestionDto> allQuestions = new ArrayList<>();

        for (Map<String, Object> poem : allPoems) {
            allQuestions.addAll(parsePoemToQuestions(poem));
        }

        return removeDuplicates(allQuestions);
    }

    /**
     * 把整首诗解析成 (上句 -> 下句) 的接龙对
     */
    private List<QuizQuestionDto> parsePoemToQuestions(Map<String, Object> poem) {
        List<QuizQuestionDto> questions = new ArrayList<>();

        String id = (String) poem.get("id");
        String title = (String) poem.get("title");
        String author = (String) poem.get("author");
        String content = (String) poem.get("content");

        if (content == null || content.isEmpty()) {
            return questions;
        }

        // 提取纯文本，去掉 <br/> 和空白
        String cleanContent = content
                .replaceAll("<br/>", "\n")
                .replaceAll("&nbsp;", "")
                .replaceAll("\\s+", "")
                .trim();

        // 按标点符号分割成句子
        String[] sentences = cleanContent.split("[。！？；：，、]");

        List<String> validSentences = new ArrayList<>();
        for (String s : sentences) {
            s = s.trim();
            if (s.length() >= 2 && !s.isEmpty()) {
                validSentences.add(s);
            }
        }

        // 生成接龙对：第i句 -> 第i+1句
        for (int i = 0; i < validSentences.size() - 1; i++) {
            String firstLine = validSentences.get(i);
            String nextLine = validSentences.get(i + 1);

            // 过滤太短的句子（少于3个字不太适合做接龙）
            if (firstLine.length() >= 3 && nextLine.length() >= 3) {
                long uniqueId = (long) (Math.abs(id.hashCode()) * 10000L + i);

                questions.add(new QuizQuestionDto(
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
     * 去除重复的接龙对（相同上句和相同下句）
     */
    private List<QuizQuestionDto> removeDuplicates(List<QuizQuestionDto> questions) {
        Map<String, QuizQuestionDto> uniqueMap = new LinkedHashMap<>();
        for (QuizQuestionDto q : questions) {
            String key = q.getFirstLine() + "||" + q.getCorrectAnswer();
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
        // 去除标点符号、空格，不区分大小写
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
}