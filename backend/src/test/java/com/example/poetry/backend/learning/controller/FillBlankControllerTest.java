package com.example.poetry.backend.learning.controller;

import com.example.poetry.backend.learning.dto.FillBlankModels;
import com.example.poetry.backend.learning.service.FillBlankService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Arrays;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class FillBlankControllerTest {

    @Mock
    private FillBlankService fillBlankService;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        FillBlankController controller = new FillBlankController(fillBlankService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        objectMapper = new ObjectMapper();
    }

    @Test
    void getRandomFillBlankQuiz_正常情况_返回题目() throws Exception {
        List<String> candidates = Arrays.asList("床", "前", "明", "月", "光");
        FillBlankModels.FillBlankQuiz mockQuiz = new FillBlankModels.FillBlankQuiz(
                100L, 1L, "静夜思", "李白", "床前明月光", candidates, "翻译内容"
        );
        when(fillBlankService.generateFillBlankQuiz()).thenReturn(mockQuiz);

        mockMvc.perform(get("/api/learning/fill-blank/random"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.workId").value(100))
                .andExpect(jsonPath("$.sentenceId").value(1))
                .andExpect(jsonPath("$.title").value("静夜思"))
                .andExpect(jsonPath("$.author").value("李白"))
                .andExpect(jsonPath("$.targetSentence").value("床前明月光"))
                .andExpect(jsonPath("$.translation").value("翻译内容"))
                .andExpect(jsonPath("$.candidateWords").isArray())
                .andExpect(jsonPath("$.candidateWords").isNotEmpty());

        verify(fillBlankService).generateFillBlankQuiz();
    }

    @Test
    void submitQuizResult_正常情况_保存记录() throws Exception {
        FillBlankModels.SubmitRequest request = new FillBlankModels.SubmitRequest(
                1L, 100L, 1L, "FILL_BLANK", true, 30,
                "{\"question\":\"test\"}", "{\"answer\":\"test\"}", "{\"correct\":\"test\"}"
        );

        mockMvc.perform(post("/api/learning/fill-blank/submit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(fillBlankService).submitResult(request);
    }

    @Test
    void getRandomFillBlankQuiz_数据库为空_返回500错误() throws Exception {
        when(fillBlankService.generateFillBlankQuiz())
                .thenThrow(new RuntimeException("数据库中没有符合条件的诗句"));

        mockMvc.perform(get("/api/learning/fill-blank/random"))
                .andExpect(status().isInternalServerError());

        verify(fillBlankService).generateFillBlankQuiz();
    }

    @Test
    void submitQuizResult_空请求_返回400错误() throws Exception {
        mockMvc.perform(post("/api/learning/fill-blank/submit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }
}
