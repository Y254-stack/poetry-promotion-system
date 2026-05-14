package com.example.poetry.backend.learning.dto;

import lombok.Data;
import java.util.List;

@Data
public class ChainRequest {
    private String startLine;           // 起始诗句（第一轮）
    private String userLine;            // 用户输入的诗句
    private String lastChar;            // 上一句的尾字，用于接龙校验
    private List<String> usedLines;     // 本局已说过的所有诗句
}
