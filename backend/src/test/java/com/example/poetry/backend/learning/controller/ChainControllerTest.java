package com.example.poetry.backend.learning.controller;

import com.example.poetry.backend.learning.dto.ChainResponse;
import com.example.poetry.backend.learning.service.ChainService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class ChainControllerTest {

    @Mock
    private ChainService chainService;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        ChainController controller = new ChainController(chainService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        objectMapper = new ObjectMapper();
    }

    @Test
    void startGame_正常情况_返回成功响应() throws Exception {
        ChainResponse mockResponse = new ChainResponse(true, "🎯 新的一局！请接：「床前明月光」", "光", null, null);
        when(chainService.startGame()).thenReturn(mockResponse);

        mockMvc.perform(post("/api/chain/start")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("🎯 新的一局！请接：「床前明月光」"))
                .andExpect(jsonPath("$.nextChar").value("光"));

        verify(chainService).startGame();
    }

    @Test
    void judge_正常情况_返回判断结果() throws Exception {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("userLine", "床前明月光");
        requestBody.put("lastChar", "床");
        requestBody.put("usedLines", List.of());

        ChainResponse mockResponse = new ChainResponse(true, "✅ 正确！", "光", null, null);
        when(chainService.judge("床前明月光", "床", List.of())).thenReturn(mockResponse);

        mockMvc.perform(post("/api/chain/judge")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("✅ 正确！"))
                .andExpect(jsonPath("$.nextChar").value("光"));

        verify(chainService).judge("床前明月光", "床", List.of());
    }

    @Test
    void judge_首字错误_返回失败响应() throws Exception {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("userLine", "床前明月光");
        requestBody.put("lastChar", "光");
        requestBody.put("usedLines", List.of());

        ChainResponse mockResponse = new ChainResponse(false, "⚠️ 诗句首字「床」不是「光」", null, null, null);
        when(chainService.judge("床前明月光", "光", List.of())).thenReturn(mockResponse);

        mockMvc.perform(post("/api/chain/judge")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("⚠️ 诗句首字「床」不是「光」"));
    }

    @Test
    void judge_重复诗句_返回失败响应() throws Exception {
        List<String> usedLines = List.of("床前明月光");
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("userLine", "床前明月光");
        requestBody.put("lastChar", "床");
        requestBody.put("usedLines", usedLines);

        ChainResponse mockResponse = new ChainResponse(false, "🔄 这句诗已经说过了", null, null, null);
        when(chainService.judge("床前明月光", "床", usedLines)).thenReturn(mockResponse);

        mockMvc.perform(post("/api/chain/judge")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("🔄 这句诗已经说过了"));
    }

    @Test
    void aiTurn_正常情况_返回AI接句() throws Exception {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("lastChar", "光");
        requestBody.put("usedLines", List.of());

        ChainResponse mockResponse = new ChainResponse(true, "AI接：「光明磊落」", "落", "光明磊落", null);
        when(chainService.aiTurn("光", List.of())).thenReturn(mockResponse);

        mockMvc.perform(post("/api/chain/ai-turn")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("AI接：「光明磊落」"))
                .andExpect(jsonPath("$.nextChar").value("落"))
                .andExpect(jsonPath("$.aiLine").value("光明磊落"));

        verify(chainService).aiTurn("光", List.of());
    }

    @Test
    void aiTurn_无法接句_返回认输() throws Exception {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("lastChar", "光");
        requestBody.put("usedLines", List.of());

        ChainResponse mockResponse = new ChainResponse(false, "我想不出来了... 认输！", null, null, null);
        when(chainService.aiTurn("光", List.of())).thenReturn(mockResponse);

        mockMvc.perform(post("/api/chain/ai-turn")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("我想不出来了... 认输！"));
    }
}
