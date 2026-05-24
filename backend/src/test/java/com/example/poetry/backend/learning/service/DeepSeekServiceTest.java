package com.example.poetry.backend.learning.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class DeepSeekServiceTest {

    @InjectMocks
    private DeepSeekService deepSeekService;

    @Test
    void parseJudgeResponse_正常JSON响应_正确解析() throws Exception {
        // 使用反射调用私有方法进行测试
        Method parseMethod = DeepSeekService.class.getDeclaredMethod(
                "parseJudgeResponse", String.class, String.class, String.class
        );
        parseMethod.setAccessible(true);

        String jsonResponse = "{\"valid\":true,\"hasKeyword\":true,\"isDuplicate\":false,\"message\":\"✅ 正确\"}";
        DeepSeekService.JudgeResult result = (DeepSeekService.JudgeResult)
                parseMethod.invoke(deepSeekService, jsonResponse, "月", "床前明月光");

        assertThat(result.valid()).isTrue();
        assertThat(result.hasKeyword()).isTrue();
        assertThat(result.isDuplicate()).isFalse();
        assertThat(result.message()).isEqualTo("✅ 正确");
    }

    @Test
    void parseJudgeResponse_JSON包含markdown标记_正确清理() throws Exception {
        Method parseMethod = DeepSeekService.class.getDeclaredMethod(
                "parseJudgeResponse", String.class, String.class, String.class
        );
        parseMethod.setAccessible(true);

        String jsonResponse = "```json\n{\"valid\":false,\"hasKeyword\":false,\"isDuplicate\":false,\"message\":\"❌ 不是古诗词\"}\n```";
        DeepSeekService.JudgeResult result = (DeepSeekService.JudgeResult)
                parseMethod.invoke(deepSeekService, jsonResponse, "月", "不是诗句");

        assertThat(result.valid()).isFalse();
        assertThat(result.hasKeyword()).isFalse();
        assertThat(result.message()).isEqualTo("❌ 不是古诗词");
    }

    @Test
    void parseJudgeResponse_响应缺少message字段_自动生成message() throws Exception {
        Method parseMethod = DeepSeekService.class.getDeclaredMethod(
                "parseJudgeResponse", String.class, String.class, String.class
        );
        parseMethod.setAccessible(true);

        String jsonResponse = "{\"valid\":false,\"hasKeyword\":false,\"isDuplicate\":false}";
        DeepSeekService.JudgeResult result = (DeepSeekService.JudgeResult)
                parseMethod.invoke(deepSeekService, jsonResponse, "月", "不是诗句");

        assertThat(result.valid()).isFalse();
        assertThat(result.message()).contains("不是古诗词原句");
    }

    @Test
    void parseJudgeResponse_无效JSON_返回默认错误() throws Exception {
        Method parseMethod = DeepSeekService.class.getDeclaredMethod(
                "parseJudgeResponse", String.class, String.class, String.class
        );
        parseMethod.setAccessible(true);

        String invalidJson = "这不是JSON格式";
        DeepSeekService.JudgeResult result = (DeepSeekService.JudgeResult)
                parseMethod.invoke(deepSeekService, invalidJson, "月", "诗句");

        assertThat(result.valid()).isFalse();
        assertThat(result.hasKeyword()).isFalse();
        assertThat(result.isDuplicate()).isFalse();
        assertThat(result.message()).isEqualTo("裁判判断失败，请重试");
    }

    @Test
    void judge_构造正确的prompt_包含所有必要信息() {
        // 注意：这个方法会实际调用API，所以通常需要Mock
        // 这里只是演示prompt构造逻辑，实际测试建议Mock HTTP调用
    }
}
