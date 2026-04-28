package com.example.poetry.backend.learning.dto;

import java.util.List;

public class QuizModels {
    public record FillBlankQuiz(
        Long workId,
        Long sentenceId,
        String title,
        String author,
        String targetSentence,
        List<String> candidateWords
    ) {}

    public record QuizSubmitRequest(
        Long userId,
        Long workId,
        Long sentenceId,
        String quizType,
        Boolean isCorrect,
        Integer durationSeconds,
        String questionPayload,
        String answerPayload,
        String correctPayload
    ) {}
}
