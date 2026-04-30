package com.example.poetry.backend.learning.controller;

import com.example.poetry.backend.learning.dto.FillBlankModels;
import com.example.poetry.backend.learning.service.FillBlankService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/learning/quiz")
public class FillBlankController {

    private final FillBlankService fillBlankService;

    public FillBlankController(FillBlankService fillBlankService) {
        this.fillBlankService = fillBlankService;
    }

    @GetMapping("/fill-blank/random")
    public FillBlankModels.FillBlankQuiz getRandomFillBlankQuiz() {
        return fillBlankService.generateFillBlankQuiz();
    }

    @PostMapping("/submit")
    public void submitQuizResult(@RequestBody FillBlankModels.SubmitRequest request) {
        fillBlankService.submitResult(request);
    }

}
