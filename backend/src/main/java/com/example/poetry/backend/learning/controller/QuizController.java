package com.example.poetry.backend.learning.controller;

import com.example.poetry.backend.learning.dto.QuizQuestionDto;
import com.example.poetry.backend.learning.service.QuizService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/quiz")
public class QuizController {

    private final QuizService quizService;

    public QuizController(QuizService quizService) {
        this.quizService = quizService;
    }

    /**
     * 获取接龙题目列表
     * GET /api/quiz/questions?limit=20
     */
    @GetMapping("/questions")
    public List<QuizQuestionDto> getQuestions(@RequestParam(defaultValue = "20") int limit) {
        return quizService.getRandomQuizQuestions(limit);
    }

}

