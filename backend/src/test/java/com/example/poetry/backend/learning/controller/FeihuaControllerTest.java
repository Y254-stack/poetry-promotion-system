package com.example.poetry.backend.learning.controller;

import com.example.poetry.backend.learning.dto.FeihuaRequest;
import com.example.poetry.backend.learning.dto.JudgeResponse;
import com.example.poetry.backend.learning.dto.AiLineResponse;
import com.example.poetry.backend.learning.service.DeepSeekService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class FeihuaControllerTest {

    private MockMvc mockMvc;

    @Mock
    private DeepSeekService deepSeekService;

    @InjectMocks
    private FeihuaController feihuaController;

    private ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(feihuaController).build();
    }

    @Test
    void judge_当所有条件满足时_返回正确() throws Exception {
        // 准备数据
        FeihuaRequest request = new FeihuaRequest("月", "床前明月光", List.of());
        DeepSeekService.JudgeResult judgeResult = new DeepSeekService.JudgeResult(true, true, false, "✅ 正确");

        when(deepSeekService.judge(eq("月"), eq("床前明月光"), anyList()))
                .thenReturn(judgeResult);

        // 执行并验证
        mockMvc.perform(post("/api/feihua/judge")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true))
                .andExpect(jsonPath("$.hasKeyword").value(true))
                .andExpect(jsonPath("$.duplicate").value(false))
                .andExpect(jsonPath("$.message").value("✅ 正确"));
    }

    @Test
    void judge_当诗句不含关键字时_返回失败() throws Exception {
        FeihuaRequest request = new FeihuaRequest("雪", "床前明月光", List.of());
        DeepSeekService.JudgeResult judgeResult = new DeepSeekService.JudgeResult(true, false, false, "⚠️ 诗句中没有「雪」字");

        when(deepSeekService.judge(eq("雪"), eq("床前明月光"), anyList()))
                .thenReturn(judgeResult);

        mockMvc.perform(post("/api/feihua/judge")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(false))
                .andExpect(jsonPath("$.hasKeyword").value(false))
                .andExpect(jsonPath("$.duplicate").value(false));
    }

    @Test
    void judge_当诗句重复时_返回失败() throws Exception {
        FeihuaRequest request = new FeihuaRequest("月", "床前明月光", List.of("床前明月光"));
        DeepSeekService.JudgeResult judgeResult = new DeepSeekService.JudgeResult(true, true, true, "🔄 这句诗已经说过了");

        when(deepSeekService.judge(eq("月"), eq("床前明月光"), anyList()))
                .thenReturn(judgeResult);

        mockMvc.perform(post("/api/feihua/judge")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(false))
                .andExpect(jsonPath("$.duplicate").value(true));
    }

    @Test
    void aiTurn_当AI有诗句时_返回诗句() throws Exception {
        FeihuaRequest request = new FeihuaRequest("月", "", List.of("床前明月光"));

        when(deepSeekService.aiTurn(eq("月"), anyList()))
                .thenReturn("疑是地上霜");

        mockMvc.perform(post("/api/feihua/ai-turn")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.line").value("疑是地上霜"))
                .andExpect(jsonPath("$.error").value(""));
    }

    @Test
    void aiTurn_当AI认输时_返回认输() throws Exception {
        FeihuaRequest request = new FeihuaRequest("罕见字", List.of());

        when(deepSeekService.aiTurn(eq("罕见字"), anyList()))
                .thenReturn("认输");

        mockMvc.perform(post("/api/feihua/ai-turn")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.line").value("认输"));
    }
}