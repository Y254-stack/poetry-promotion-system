package com.example.poetry.backend.learning.dto;

import java.util.List;

public class FillBlankModels {
    public record FillBlankQuiz(
        Long workId,
        Long sentenceId,
        String title,
        String author,
        String targetSentence,
        List<String> candidateWords,
        String translation
    ) {}

    public record SubmitRequest(
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