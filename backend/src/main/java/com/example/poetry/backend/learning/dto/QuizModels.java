package com.example.poetry.backend.learning.dto;

import java.util.List;

public class QuizModels {

    // 接龙题目
    public record QuizQuestion(
            Long id,
            String firstLine,
            String correctAnswer,
            String sourceTitle,
            String sourceAuthor
    ) {}

    // 提交请求
    public record SubmitRequest(
            Long userId,
            Long questionId,
            String userAnswer,
            String correctAnswer,
            String quizType,
            Boolean isCorrect,
            Integer durationSeconds,
            String questionPayload,
            String answerPayload,
            String correctPayload
    ) {}

    // 提交响应
    public record SubmitResponse(
            boolean correct,
            String message
    ) {}

    // 批量提交请求
    public record BatchSubmitRequest(
            List<SubmitRequest> answers,
            Integer userId,
            Integer durationSeconds
    ) {}

    // 批量提交响应
    public record BatchSubmitResponse(
            int correctCount,
            int totalCount,
            List<Boolean> results
    ) {}
}
