package com.example.poetry.backend.learning.controller;

import com.example.poetry.backend.learning.dto.QuizModels;
import com.example.poetry.backend.learning.service.QuizService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/learning/quiz")
public class QuizController {

    private final QuizService quizService;

    public QuizController(QuizService quizService) {
        this.quizService = quizService;
    }

    @GetMapping("/fill-blank/random")
    public QuizModels.FillBlankQuiz getRandomFillBlankQuiz() {
        return quizService.generateFillBlankQuiz();
    }

    @PostMapping("/submit")
    public void submitQuizResult(@RequestBody QuizModels.QuizSubmitRequest request) {
        quizService.submitResult(request);
    }
}
