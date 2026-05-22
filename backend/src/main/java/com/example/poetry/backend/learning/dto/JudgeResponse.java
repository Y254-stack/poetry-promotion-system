package com.example.poetry.backend.learning.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class JudgeResponse {
    private boolean valid;        // 是否合法诗句
    private boolean hasKeyword;   // 是否含关键字
    private boolean isDuplicate;  // 是否重复
    private String message;       // 提示信息
}
