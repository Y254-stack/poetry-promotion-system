package com.example.poetry.backend.learning.dto;

import lombok.Data;
import java.util.List;

@Data
public class FeihuaRequest {
    private String keyword;           // 当前关键字，如"花"
    private String userLine;          // 用户输入的诗句（裁判接口用）
    private List<String> usedLines;   // 本局已说过的所有诗句
}
