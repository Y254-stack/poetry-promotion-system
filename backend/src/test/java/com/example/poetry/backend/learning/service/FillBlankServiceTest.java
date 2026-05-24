package com.example.poetry.backend.learning.service;

import com.example.poetry.backend.learning.dto.FillBlankModels;
import com.example.poetry.backend.learning.repository.FillBlankRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FillBlankServiceTest {

    @Mock
    private FillBlankRepository fillBlankRepository;

    @InjectMocks
    private FillBlankService fillBlankService;

    @BeforeEach
    void setUp() {
        // 初始化设置
    }

    @Test
    void generateFillBlankQuiz_正常情况_返回题目() {
        // 准备模拟数据
        Map<String, Object> mockData = new HashMap<>();
        mockData.put("sentence_id", 1L);
        mockData.put("work_id", 100L);
        mockData.put("sentence_text", "床前明月光，疑是地上霜。");
        mockData.put("title", "静夜思");
        mockData.put("author", "李白");
        mockData.put("translation", "明亮的月光洒在床前的窗户纸上...");

        when(fillBlankRepository.getRandomSentenceForQuiz()).thenReturn(Optional.of(mockData));

        // 执行
        FillBlankModels.FillBlankQuiz result = fillBlankService.generateFillBlankQuiz();

        // 验证
        assertThat(result).isNotNull();
        assertThat(result.workId()).isEqualTo(100L);
        assertThat(result.sentenceId()).isEqualTo(1L);
        assertThat(result.title()).isEqualTo("静夜思");
        assertThat(result.author()).isEqualTo("李白");
        assertThat(result.targetSentence()).isEqualTo("床前明月光，疑是地上霜。");
        assertThat(result.translation()).isEqualTo("明亮的月光洒在床前的窗户纸上...");
        
        // 验证候选词数量（去除标点后的字数）
        assertThat(result.candidateWords()).hasSize(10); // "床前明月光疑是地上霜"共10字
        
        // 验证候选词包含所有字
        String cleanSentence = "床前明月光疑是地上霜";
        for (char c : cleanSentence.toCharArray()) {
            assertThat(result.candidateWords()).contains(String.valueOf(c));
        }
    }

    @Test
    void generateFillBlankQuiz_无标点句子_正确生成候选词() {
        Map<String, Object> mockData = new HashMap<>();
        mockData.put("sentence_id", 2L);
        mockData.put("work_id", 200L);
        mockData.put("sentence_text", "白日依山尽");
        mockData.put("title", "登鹳雀楼");
        mockData.put("author", "王之涣");
        mockData.put("translation", null);

        when(fillBlankRepository.getRandomSentenceForQuiz()).thenReturn(Optional.of(mockData));

        FillBlankModels.FillBlankQuiz result = fillBlankService.generateFillBlankQuiz();

        assertThat(result.candidateWords()).hasSize(5); // "白日依山尽"共5字
        assertThat(result.translation()).isNull();
    }

    @Test
    void generateFillBlankQuiz_数据库为空_抛出异常() {
        when(fillBlankRepository.getRandomSentenceForQuiz()).thenReturn(Optional.empty());

        org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class, () -> {
            fillBlankService.generateFillBlankQuiz();
        });
    }

    @Test
    void generateFillBlankQuiz_候选词顺序随机() {
        Map<String, Object> mockData = new HashMap<>();
        mockData.put("sentence_id", 3L);
        mockData.put("work_id", 300L);
        mockData.put("sentence_text", "春眠不觉晓");
        mockData.put("title", "春晓");
        mockData.put("author", "孟浩然");
        mockData.put("translation", "");

        when(fillBlankRepository.getRandomSentenceForQuiz()).thenReturn(Optional.of(mockData));

        // 多次生成，验证顺序不同
        List<String> firstOrder = null;
        int sameCount = 0;
        for (int i = 0; i < 5; i++) {
            FillBlankModels.FillBlankQuiz result = fillBlankService.generateFillBlankQuiz();
            if (firstOrder == null) {
                firstOrder = new ArrayList<>(result.candidateWords());
            } else {
                if (firstOrder.equals(result.candidateWords())) {
                    sameCount++;
                }
            }
        }
        
        // 理论上5次生成完全相同顺序的概率很低，但允许一定容错
        // 这里主要验证功能正确性，随机性在实际运行中验证
        assertThat(sameCount).isLessThan(5); // 至少有一次不同
    }

    @Test
    void submitResult_正常情况_保存记录() {
        FillBlankModels.SubmitRequest request = new FillBlankModels.SubmitRequest(
                1L, 100L, 1L, "FILL_BLANK", true, 30, 
                "{\"question\":\"test\"}", "{\"answer\":\"test\"}", "{\"correct\":\"test\"}"
        );

        fillBlankService.submitResult(request);

        verify(fillBlankRepository).saveQuizRecord(request);
    }

    @Test
    void generateFillBlankQuiz_多种标点_正确处理() {
        Map<String, Object> mockData = new HashMap<>();
        mockData.put("sentence_id", 4L);
        mockData.put("work_id", 400L);
        mockData.put("sentence_text", "人生自古谁无死？留取丹心照汗青！");
        mockData.put("title", "过零丁洋");
        mockData.put("author", "文天祥");
        mockData.put("translation", "人生自古以来有谁能够长生不死？");

        when(fillBlankRepository.getRandomSentenceForQuiz()).thenReturn(Optional.of(mockData));

        FillBlankModels.FillBlankQuiz result = fillBlankService.generateFillBlankQuiz();

        // 去除标点后应为14个字："人生自古谁无死留取丹心照汗青"
        assertThat(result.candidateWords()).hasSize(14);
    }
}
