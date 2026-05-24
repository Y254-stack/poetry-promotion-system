package com.example.poetry.backend.learning.service;

import com.example.poetry.backend.learning.dto.ChainResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class ChainServiceTest {

    @InjectMocks
    private ChainService chainService;

    @BeforeEach
    void setUp() {
        // 使用空API key初始化，测试离线功能
        chainService = new ChainService("test-key");
    }

    // ==================== 游戏开始测试 ====================

    @Test
    void startGame_正常情况_返回起始诗句和尾字() {
        ChainResponse response = chainService.startGame();

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getMessage()).contains("「");
        assertThat(response.getMessage()).contains("」");
        assertThat(response.getNextChar()).isNotEmpty();
    }

    // ==================== 首字尾字提取测试 ====================

    @Test
    void getFirstChar_正常诗句_正确提取首字() {
        ChainResponse response = chainService.judge("床前明月光", "床", List.of());
        assertThat(response.isSuccess()).isTrue();
    }

    @Test
    void getFirstChar_带标点开头_正确提取首字() {
        ChainResponse response = chainService.judge("，床前明月光", "床", List.of());
        assertThat(response.isSuccess()).isTrue();
    }

    @Test
    void getLastChar_正常诗句_正确提取尾字() {
        ChainResponse response = chainService.judge("床前明月光", "床", List.of());
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getNextChar()).isEqualTo("光");
    }

    @Test
    void getLastChar_带标点结尾_正确提取尾字() {
        ChainResponse response = chainService.judge("床前明月光。", "床", List.of());
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getNextChar()).isEqualTo("光");
    }

    @Test
    void getLastChar_带多种标点_正确提取尾字() {
        ChainResponse response = chainService.judge("床前明月光！", "床", List.of());
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getNextChar()).isEqualTo("光");
    }

    // ==================== 接龙规则校验测试 ====================

    @Test
    void judge_首字正确_返回成功() {
        ChainResponse response = chainService.judge("光风霁月", "光", List.of());
        assertThat(response.isSuccess()).isTrue();
    }

    @Test
    void judge_首字错误_返回失败() {
        ChainResponse response = chainService.judge("床前明月光", "光", List.of());
        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getMessage()).contains("首字");
    }

    @Test
    void judge_重复诗句_返回失败() {
        List<String> usedLines = new ArrayList<>();
        usedLines.add("床前明月光");
        
        ChainResponse response = chainService.judge("床前明月光", "床", usedLines);
        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getMessage()).contains("已经说过");
    }

    @Test
    void judge_空输入_返回失败() {
        ChainResponse response = chainService.judge("", "床", List.of());
        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getMessage()).contains("请输入诗句");
    }

    @Test
    void judge_null输入_返回失败() {
        ChainResponse response = chainService.judge(null, "床", List.of());
        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getMessage()).contains("请输入诗句");
    }

    // ==================== AI回合测试（离线模式） ====================

    @Test
    void aiTurn_离线模式_返回认输() {
        ChainResponse response = chainService.aiTurn("光", List.of());
        assertThat(response.isSuccess()).isFalse();
    }

    // ==================== 端到端接龙测试 ====================

    @Test
    void chainFlow_完整回合_正常进行() {
        ChainResponse startResponse = chainService.startGame();
        assertThat(startResponse.isSuccess()).isTrue();
        assertThat(startResponse.getNextChar()).isNotEmpty();
        
        String lastChar = startResponse.getNextChar();
        
        ChainResponse judgeResponse = chainService.judge(lastChar + "明几时有", lastChar, List.of());
        assertThat(judgeResponse.isSuccess()).isTrue();
        
        ChainResponse aiResponse = chainService.aiTurn(judgeResponse.getNextChar(), List.of());
    }
}
