package com.example.poetry.backend.learning.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FeihuaRequest {
    private String keyword;           // 当前关键字，如"花"
    private String userLine;          // 用户输入的诗句（裁判接口用）
    private List<String> usedLines;   // 本局已说过的所有诗句
}
