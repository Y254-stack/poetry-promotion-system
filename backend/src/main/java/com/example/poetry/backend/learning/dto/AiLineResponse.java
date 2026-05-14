package com.example.poetry.backend.learning.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AiLineResponse {
    private String line;      // AI出的诗句
    private String source;    // 出处（可选）
}
