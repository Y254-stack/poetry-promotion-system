package com.example.poetry.backend.learning.controller;

import com.example.poetry.backend.learning.dto.QuizModels;
import com.example.poetry.backend.learning.service.QuizService;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class QuizControllerTest {

    private MockMvc mockMvc;

    @Mock
    private QuizService quizService;

    @InjectMocks
    private QuizController quizController;

    private ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(quizController).build();
    }

    @Test
    void getQuizQuestions_默认参数_返回20道题() throws Exception {
        List<QuizModels.QuizQuestion> mockQuestions = List.of(
                new QuizModels.QuizQuestion(1L, "床前明月光", "疑是地上霜", "静夜思", "李白"),
                new QuizModels.QuizQuestion(2L, "举头望明月", "低头思故乡", "静夜思", "李白")
        );

        when(quizService.generateQuizQuestions(20)).thenReturn(mockQuestions);

        mockMvc.perform(get("/api/quiz/questions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].firstLine").value("床前明月光"))
                .andExpect(jsonPath("$[0].correctAnswer").value("疑是地上霜"));
    }

    @Test
    void getQuizQuestions_自定义参数_返回指定数量题目() throws Exception {
        List<QuizModels.QuizQuestion> mockQuestions = List.of(
                new QuizModels.QuizQuestion(1L, "白日依山尽", "黄河入海流", "登鹳雀楼", "王之涣")
        );

        when(quizService.generateQuizQuestions(5)).thenReturn(mockQuestions);

        mockMvc.perform(get("/api/quiz/questions?limit=5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void submitAnswer_正确答案_返回成功() throws Exception {
        QuizModels.SubmitRequest request = new QuizModels.SubmitRequest(
                1L, 1L, "疑是地上霜", "疑是地上霜",
                "POEM_CHAIN", null, 5, null, null, null
        );

        QuizModels.SubmitResponse mockResponse = new QuizModels.SubmitResponse(true, "回答正确！");

        when(quizService.submitResult(any(QuizModels.SubmitRequest.class)))
                .thenReturn(mockResponse);

        mockMvc.perform(post("/api/quiz/submit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.correct").value(true))
                .andExpect(jsonPath("$.message").value("回答正确！"));
    }

    @Test
    void submitAnswer_错误答案_返回失败() throws Exception {
        QuizModels.SubmitRequest request = new QuizModels.SubmitRequest(
                1L, 1L, "错误答案", "疑是地上霜",
                "POEM_CHAIN", null, 5, null, null, null
        );

        QuizModels.SubmitResponse mockResponse = new QuizModels.SubmitResponse(false, "回答错误，再接再厉");

        when(quizService.submitResult(any(QuizModels.SubmitRequest.class)))
                .thenReturn(mockResponse);

        mockMvc.perform(post("/api/quiz/submit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.correct").value(false))
                .andExpect(jsonPath("$.message").value("回答错误，再接再厉"));
    }
}