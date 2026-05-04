package com.example.poetry.backend.learning.service;

import com.example.poetry.backend.learning.dto.FillBlankModels;
import com.example.poetry.backend.learning.repository.FillBlankRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class FillBlankService {

    private static final Logger log = LoggerFactory.getLogger(FillBlankService.class);
    private final FillBlankRepository fillBlankRepository;

    public FillBlankService(FillBlankRepository fillBlankRepository) {
        this.fillBlankRepository = fillBlankRepository;
    }

    public FillBlankModels.FillBlankQuiz generateFillBlankQuiz() {
        log.info("========== 开始生成随机选词填空题 ==========");
        long totalStart = System.currentTimeMillis();

        log.info("步骤1: 开始从数据库获取随机句子...");
        long dbStart = System.currentTimeMillis();
        
        Optional<Map<String, Object>> dataOpt = fillBlankRepository.getRandomSentenceForQuiz();
        
        Map<String, Object> data = dataOpt.orElseThrow(() -> {
            log.error("数据库中没有符合条件的诗句，请确保执行了数据初始化脚本！");
            return new RuntimeException("数据库中没有符合条件的诗句，请确保执行了数据初始化脚本。");
        });

        long dbEnd = System.currentTimeMillis();
        log.info("步骤1完成: SQL查询耗时: {}ms", (dbEnd - dbStart));

        log.info("步骤2: 解析数据...");
        Object sentenceIdObj = data.get("sentence_id");
        Long sentenceId = (sentenceIdObj instanceof Number) ? ((Number) sentenceIdObj).longValue() : 0L;

        Object workIdObj = data.get("work_id");
        Long workId = (workIdObj instanceof Number) ? ((Number) workIdObj).longValue() : 0L;

        String sentence = (String) data.get("sentence_text");
        String title = (String) data.get("title");
        String author = (String) data.get("author");
        String translation = (String) data.get("translation");
        
        log.info("解析结果: workId={}, sentenceId={}, title={}, author={}, hasTranslation={}", workId, sentenceId, title, author, translation != null && !translation.isEmpty());

        log.info("步骤3: 生成候选词...");
        String cleanSentence = sentence.replaceAll("[，。？！；：、]", "");
        List<String> candidates = new ArrayList<>();
        for (char c : cleanSentence.toCharArray()) {
            candidates.add(String.valueOf(c));
        }
        log.info("原始句子: {}, 候选词数量: {}", sentence, candidates.size());
        
        Collections.shuffle(candidates);
        log.info("打乱后的候选词: {}", candidates);

        long totalEnd = System.currentTimeMillis();
        log.info("========== 题目生成完成 ==========");
        log.info("总耗时: {}ms, 句子长度: {}字", (totalEnd - totalStart), cleanSentence.length());
        
        return new FillBlankModels.FillBlankQuiz(workId, sentenceId, title, author, sentence, candidates, translation);
    }

    public void submitResult(FillBlankModels.SubmitRequest request) {
        fillBlankRepository.saveQuizRecord(request);
    }
}