package com.example.poetry.backend.learning.controller;

import com.example.poetry.backend.learning.dto.*;
import com.example.poetry.backend.learning.service.DeepSeekService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/feihua")
public class FeihuaController {

    private static final Logger log = LoggerFactory.getLogger(FeihuaController.class);
    private final DeepSeekService deepSeekService;

    public FeihuaController(DeepSeekService deepSeekService) {
        this.deepSeekService = deepSeekService;
    }

    @PostMapping("/judge")
    public JudgeResponse judge(@RequestBody FeihuaRequest request) {
        log.info("裁判请求 - 关键字: {}, 用户输入: {}", request.getKeyword(), request.getUserLine());

        var result = deepSeekService.judge(
                request.getKeyword(),
                request.getUserLine(),
                request.getUsedLines()
        );

        log.info("判断结果: valid={}, hasKeyword={}, isDuplicate={}, message={}",
                result.valid(), result.hasKeyword(), result.isDuplicate(), result.message());

        return new JudgeResponse(
                result.valid() && result.hasKeyword() && !result.isDuplicate(),
                result.hasKeyword(),
                result.isDuplicate(),
                result.message()
        );
    }

    @PostMapping("/ai-turn")
    public AiLineResponse aiTurn(@RequestBody FeihuaRequest request) {
        log.info("AI出题请求 - 关键字: {}, 已用诗句数: {}", request.getKeyword(), request.getUsedLines().size());

        String line = deepSeekService.aiTurn(request.getKeyword(), request.getUsedLines());

        return new AiLineResponse(line, "");
    }
}