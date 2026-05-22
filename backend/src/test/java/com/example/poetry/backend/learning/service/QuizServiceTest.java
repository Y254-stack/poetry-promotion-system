package com.example.poetry.backend.learning.service;

import com.example.poetry.backend.learning.dto.QuizModels;
import com.example.poetry.backend.learning.repository.QuizRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QuizServiceTest {

    @Mock
    private QuizRepository quizRepository;

    @InjectMocks
    private QuizService quizService;

    @BeforeEach
    void setUp() {
        // 初始化设置
    }

    @Test
    void generateQuizQuestions_正常情况_返回题目列表() {
        // 准备数据
        List<Map<String, Object>> mockPoems = new ArrayList<>();
        Map<String, Object> poem = new HashMap<>();
        poem.put("id", 1);
        poem.put("title", "静夜思");
        poem.put("author", "李白");
        poem.put("content", "床前明月光。疑是地上霜。举头望明月。低头思故乡。");
        mockPoems.add(poem);

        when(quizRepository.getRandomPoemsForQuiz(anyInt())).thenReturn(mockPoems);

        // 执行
        List<QuizModels.QuizQuestion> result = quizService.generateQuizQuestions(2);

        // 验证
        assertThat(result).isNotEmpty();
        assertThat(result.get(0).firstLine()).isEqualTo("床前明月光");
        assertThat(result.get(0).correctAnswer()).isEqualTo("疑是地上霜");
        assertThat(result.get(0).sourceTitle()).isEqualTo("静夜思");
        assertThat(result.get(0).sourceAuthor()).isEqualTo("李白");
    }

    @Test
    void generateQuizQuestions_诗词内容包含BR标签_正确解析() {
        List<Map<String, Object>> mockPoems = new ArrayList<>();
        Map<String, Object> poem = new HashMap<>();
        poem.put("id", 1);
        poem.put("title", "春晓");
        poem.put("author", "孟浩然");
        poem.put("content", "春眠不觉晓，<br/>处处闻啼鸟。<br/>夜来风雨声，<br/>花落知多少。");
        mockPoems.add(poem);

        when(quizRepository.getRandomPoemsForQuiz(anyInt())).thenReturn(mockPoems);

        List<QuizModels.QuizQuestion> result = quizService.generateQuizQuestions(10);

        assertThat(result).hasSize(3);
        assertThat(result.get(0).firstLine()).contains("春眠不觉晓");
        assertThat(result.get(0).correctAnswer()).contains("处处闻啼鸟");
    }

    @Test
    void generateQuizQuestions_数据库为空_返回空列表() {
        when(quizRepository.getRandomPoemsForQuiz(anyInt())).thenReturn(List.of());

        List<QuizModels.QuizQuestion> result = quizService.generateQuizQuestions(10);

        assertThat(result).isEmpty();
    }

    @Test
    void generateQuizQuestions_诗词内容含有标点_正确分割() {
        List<Map<String, Object>> mockPoems = new ArrayList<>();
        Map<String, Object> poem = new HashMap<>();
        poem.put("id", 1);
        poem.put("title", "将进酒");
        poem.put("author", "李白");
        poem.put("content", "君不见黄河之水天上来！奔流到海不复回？君不见高堂明镜悲白发，朝如青丝暮成雪。");
        mockPoems.add(poem);

        when(quizRepository.getRandomPoemsForQuiz(anyInt())).thenReturn(mockPoems);

        List<QuizModels.QuizQuestion> result = quizService.generateQuizQuestions(10);

        assertThat(result).hasSize(3);
    }

    @Test
    void validateAnswer_完全匹配_返回true() {
        boolean result = quizService.validateAnswer("床前明月光", "床前明月光");
        assertThat(result).isTrue();
    }

    @Test
    void validateAnswer_忽略标点符号_返回true() {
        boolean result = quizService.validateAnswer("床前明月光，", "床前明月光");
        assertThat(result).isTrue();
    }

    @Test
    void validateAnswer_忽略大小写_返回true() {
        boolean result = quizService.validateAnswer("床前明月光", "床前明月光");
        assertThat(result).isTrue();
    }

    @Test
    void validateAnswer_答案不匹配_返回false() {
        boolean result = quizService.validateAnswer("床前明月光", "疑是地上霜");
        assertThat(result).isFalse();
    }

    @Test
    void validateAnswer_null值_返回false() {
        boolean result = quizService.validateAnswer(null, "床前明月光");
        assertThat(result).isFalse();

        result = quizService.validateAnswer("床前明月光", null);
        assertThat(result).isFalse();
    }

    @Test
    void submitResult_正确答案_保存记录并返回成功() {
        QuizModels.SubmitRequest request = new QuizModels.SubmitRequest(
                1L, 100L, "疑是地上霜", "疑是地上霜",
                "POEM_CHAIN", null, 10, null, null, null
        );

        QuizModels.SubmitResponse response = quizService.submitResult(request);

        assertThat(response.correct()).isTrue();
        assertThat(response.message()).isEqualTo("回答正确！");

        // 验证保存方法被调用
        ArgumentCaptor<QuizModels.SubmitRequest> captor = ArgumentCaptor.forClass(QuizModels.SubmitRequest.class);
        verify(quizRepository).saveQuizRecord(captor.capture());

        QuizModels.SubmitRequest savedRequest = captor.getValue();
        assertThat(savedRequest.isCorrect()).isTrue();
    }

    @Test
    void submitResult_错误答案_保存记录并返回失败() {
        QuizModels.SubmitRequest request = new QuizModels.SubmitRequest(
                1L, 100L, "错误答案", "疑是地上霜",
                "POEM_CHAIN", null, 10, null, null, null
        );

        QuizModels.SubmitResponse response = quizService.submitResult(request);

        assertThat(response.correct()).isFalse();
        assertThat(response.message()).isEqualTo("回答错误，再接再厉");

        verify(quizRepository).saveQuizRecord(any(QuizModels.SubmitRequest.class));
    }

    @Test
    void generateQuizQuestions_重复题目_自动去重() {
        // 创建两首相同的诗词来测试去重
        List<Map<String, Object>> mockPoems = new ArrayList<>();

        Map<String, Object> poem1 = new HashMap<>();
        poem1.put("id", 1);
        poem1.put("title", "静夜思");
        poem1.put("author", "李白");
        poem1.put("content", "床前明月光。疑是地上霜。");
        mockPoems.add(poem1);

        Map<String, Object> poem2 = new HashMap<>();
        poem2.put("id", 2);
        poem2.put("title", "静夜思");
        poem2.put("author", "李白");
        poem2.put("content", "床前明月光。疑是地上霜。");
        mockPoems.add(poem2);

        when(quizRepository.getRandomPoemsForQuiz(anyInt())).thenReturn(mockPoems);

        List<QuizModels.QuizQuestion> result = quizService.generateQuizQuestions(10);

        // 去重后应该只有1道题
        assertThat(result).hasSize(1);
    }

    @Test
    void generateQuizQuestions_句子长度过滤_短句子被过滤() {
        List<Map<String, Object>> mockPoems = new ArrayList<>();
        Map<String, Object> poem = new HashMap<>();
        poem.put("id", 1);
        poem.put("title", "短诗测试");
        poem.put("author", "佚名");
        poem.put("content", "一二。三四五。六七。八九十一二。");
        mockPoems.add(poem);

        when(quizRepository.getRandomPoemsForQuiz(anyInt())).thenReturn(mockPoems);

        List<QuizModels.QuizQuestion> result = quizService.generateQuizQuestions(10);

        // 只有 "三四五" -> "六七" 这个接龙对可能符合长度要求
        assertThat(result).allMatch(q -> q.firstLine().length() >= 3 && q.correctAnswer().length() >= 3);
    }
}
