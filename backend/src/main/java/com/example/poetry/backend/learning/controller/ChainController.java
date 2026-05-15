package com.example.poetry.backend.learning.controller;

import com.example.poetry.backend.learning.dto.ChainRequest;
import com.example.poetry.backend.learning.dto.ChainResponse;
import com.example.poetry.backend.learning.service.ChainService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chain")
public class ChainController {

    private static final Logger log = LoggerFactory.getLogger(ChainController.class);
    private final ChainService chainService;

    public ChainController(ChainService chainService) {
        this.chainService = chainService;
    }

    @PostMapping("/start")
    public ChainResponse startGame(@RequestBody ChainRequest request) {
        log.info("开始接龙游戏");
        return chainService.startGame();
    }

    @PostMapping("/judge")
    public ChainResponse judge(@RequestBody ChainRequest request) {
        log.info("裁判请求 - 用户输入: {}, 尾字: {}", request.getUserLine(), request.getLastChar());
        return chainService.judge(request.getUserLine(), request.getLastChar(), request.getUsedLines());
    }

    @PostMapping("/ai-turn")
    public ChainResponse aiTurn(@RequestBody ChainRequest request) {
        log.info("AI接龙请求 - 尾字: {}, 已用诗句数: {}", request.getLastChar(), request.getUsedLines().size());
        return chainService.aiTurn(request.getLastChar(), request.getUsedLines());
    }
}
