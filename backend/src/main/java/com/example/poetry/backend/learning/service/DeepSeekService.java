package com.example.poetry.backend.learning.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

@Service
public class DeepSeekService {

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String apiKey;
    private final String apiUrl = "https://api.deepseek.com/v1/chat/completions";

    public DeepSeekService(@Value("${deepseek.api.key}") String apiKey) {
        this.apiKey = apiKey;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();
    }

    public JudgeResult judge(String keyword, String userLine, List<String> usedLines) {
        String prompt = String.format("""
        你是飞花令裁判。当前关键字是「%s」。
        已说过的诗句：%s
        
        用户输入：「%s」
        
        只输出一行JSON，不要有任何其他内容，包括markdown格式。
        
        输出格式必须严格如下：
        {"valid":false,"hasKeyword":false,"isDuplicate":false,"message":"❌ xxx不是古诗词"}
        
        message填写规则（必须写具体内容，不能写"判断完成"）：
        - 不是真实诗句：写「❌ 输入内容不是古诗词原句」
        - 不含关键字：写「⚠️ 诗句中没有「%s」字」
        - 重复：写「🔄 这句诗已经说过了」
        - 正确：写「✅ 正确」
        """, keyword, String.join("、", usedLines), userLine, keyword);

        String response = callDeepSeek(prompt);
        System.out.println("DeepSeek 返回原始内容: " + response);

        return parseJudgeResponse(response, keyword, userLine);
    }

    public String aiTurn(String keyword, List<String> usedLines) {
        String prompt = String.format("""
            你是飞花令玩家。当前关键字是「%s」。
            已说过的诗句：%s
            
            请输出一句真实存在的古诗词原句，要求：
            1. 必须包含关键字「%s」
            2. 不能与已说过的重复
            3. 只输出诗句本身，不要有任何解释
            4. 如果实在想不出来，输出「认输」
            """, keyword, String.join("、", usedLines), keyword);

        String response = callDeepSeek(prompt);
        System.out.println("DeepSeek AI返回: " + response);

        String line = response.trim()
                .replaceAll("[\"']", "")
                .replaceAll("^[一二三四五、]*", "");

        if (line.contains("认输") || line.isEmpty()) {
            return "认输";
        }
        return line;
    }

    private String callDeepSeek(String prompt) {
        try {
            Map<String, Object> requestBody = Map.of(
                    "model", "deepseek-chat",
                    "messages", List.of(Map.of("role", "user", "content", prompt)),
                    "temperature", 0.1,
                    "max_tokens", 200
            );

            String json = objectMapper.writeValueAsString(requestBody);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .timeout(Duration.ofSeconds(30))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                System.out.println("API调用失败，状态码: " + response.statusCode());
                return "";
            }

            JsonNode root = objectMapper.readTree(response.body());
            return root.path("choices").get(0).path("message").path("content").asText();

        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    private JudgeResult parseJudgeResponse(String response, String keyword, String userLine) {
        try {
            // 清理响应：去掉可能的 markdown 代码块
            String cleanResponse = response.trim();
            if (cleanResponse.startsWith("```json")) {
                cleanResponse = cleanResponse.substring(7);
            }
            if (cleanResponse.startsWith("```")) {
                cleanResponse = cleanResponse.substring(3);
            }
            if (cleanResponse.endsWith("```")) {
                cleanResponse = cleanResponse.substring(0, cleanResponse.length() - 3);
            }
            cleanResponse = cleanResponse.trim();

            System.out.println("清理后的JSON: " + cleanResponse);

            JsonNode json = objectMapper.readTree(cleanResponse);

            boolean valid = json.has("valid") && json.path("valid").asBoolean();
            boolean hasKeyword = json.has("hasKeyword") && json.path("hasKeyword").asBoolean();
            boolean isDuplicate = json.has("isDuplicate") && json.path("isDuplicate").asBoolean();

            // 获取 message，如果没有则根据判断结果生成
            String message = json.has("message") ? json.path("message").asText() : "";
            if (message.isEmpty() || message.equals("判断完成")) {
                if (!valid) {
                    message = "❌ \"" + userLine + "\" 不是古诗词原句";
                } else if (!hasKeyword) {
                    message = "⚠️ 诗句中没有「" + keyword + "」字";
                } else if (isDuplicate) {
                    message = "🔄 这句诗已经说过了";
                } else {
                    message = "✅ 正确！轮到AI了";
                }
            }

            return new JudgeResult(valid, hasKeyword, isDuplicate, message);
        } catch (Exception e) {
            e.printStackTrace();
            return new JudgeResult(false, false, false, "裁判判断失败，请重试");
        }
    }

    public record JudgeResult(boolean valid, boolean hasKeyword, boolean isDuplicate, String message) {}
}