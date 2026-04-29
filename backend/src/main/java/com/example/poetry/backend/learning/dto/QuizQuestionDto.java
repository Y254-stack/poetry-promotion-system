package com.example.poetry.backend.learning.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class QuizQuestionDto {
    private Long id;           // 题目ID（用于提交记录）
    private String firstLine;  // 上句
    private String correctAnswer; // 标准下句
    private String sourceTitle;    // 来源诗词标题（可选，展示用）
    private String sourceAuthor;   // 作者
}
