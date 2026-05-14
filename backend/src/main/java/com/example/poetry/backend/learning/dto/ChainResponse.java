package com.example.poetry.backend.learning.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChainResponse {
    private boolean success;            // 接龙是否成功
    private String message;             // 提示信息
    private String nextChar;            // 下一个接龙字（尾字）
    private String aiLine;              // AI接的诗句（如果是AI回合）
    private String source;              // 诗句来源（作者、诗名）
}
