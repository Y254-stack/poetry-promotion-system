package com.example.poetry.backend.learning.dto;

import java.util.List;

public class QuizModels {
    public record FillBlankQuiz(
        Long workId,
        String title,
        String author,
        String targetSentence,
        List<String> candidateWords
    ) {}

    public record QuizSubmitRequest(
        Long userId,
        Long workId,
        String quizType,
        Boolean isCorrect,
        Integer durationSeconds
    ) {}
}
