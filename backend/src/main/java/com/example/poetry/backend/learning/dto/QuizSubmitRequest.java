package com.example.poetry.backend.learning.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// 辅助类
@Data
@NoArgsConstructor
@AllArgsConstructor
public class QuizSubmitRequest {
    private Long questionId;
    private String userAnswer;
    private String correctAnswer;
    private Integer userId;
}
