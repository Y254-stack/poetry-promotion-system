package com.example.poetry.backend.learning.controller;

import com.example.poetry.backend.learning.dto.QuizModels;
import com.example.poetry.backend.learning.service.QuizService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/quiz")
public class QuizController {

    private static final Logger log = LoggerFactory.getLogger(QuizController.class);
    private final QuizService quizService;

    public QuizController(QuizService quizService) {
        this.quizService = quizService;
    }

    /**
     * 获取接龙题目列表
     * GET /api/quiz/questions?limit=20
     */
    @GetMapping("/questions")
    public List<QuizModels.QuizQuestion> getQuizQuestions(@RequestParam(defaultValue = "20") int limit) {
        log.info("收到获取接龙题目请求, limit: {}", limit);
        return quizService.generateQuizQuestions(limit);
    }

    /**
     * 提交单题答案
     * POST /api/quiz/submit
     */
    @PostMapping("/submit")
    public QuizModels.SubmitResponse submitAnswer(@RequestBody QuizModels.SubmitRequest request) {
        log.info("收到提交答案请求, questionId: {}, userAnswer: {}", request.questionId(), request.userAnswer());
        return quizService.submitResult(request);
    }
}