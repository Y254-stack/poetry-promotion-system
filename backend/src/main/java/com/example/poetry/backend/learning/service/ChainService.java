package com.example.poetry.backend.learning.service;

import com.example.poetry.backend.learning.dto.ChainResponse;
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
public class ChainService {

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String apiKey;
    private final String apiUrl = "https://api.deepseek.com/v1/chat/completions";

    // 预设的起始诗句
    private final List<String> startLines = List.of(
            "床前明月光",
            "春眠不觉晓",
            "白日依山尽",
            "黄河入海流",
            "举头望明月",
            "锄禾日当午",
            "红豆生南国",
            "窗前明月光",
            "千山鸟飞绝",
            "白日依山尽"
    );

    public ChainService(@Value("${deepseek.api.key}") String apiKey) {
        this.apiKey = apiKey;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();
    }

    public ChainResponse startGame() {
        // 随机选择一句起始诗
        String startLine = startLines.get((int) (Math.random() * startLines.size()));
        // 获取尾字
        String lastChar = getLastChar(startLine);
        
        return new ChainResponse(true, "🎯 新的一局！请接：「" + startLine + "」", lastChar, null, null);
    }

    public ChainResponse judge(String userLine, String lastChar, List<String> usedLines) {
        if (userLine == null || userLine.trim().isEmpty()) {
            return new ChainResponse(false, "❌ 请输入诗句", null, null, null);
        }

        userLine = userLine.trim();

        // 检查是否重复
        if (usedLines.contains(userLine)) {
            return new ChainResponse(false, "🔄 这句诗已经说过了", null, null, null);
        }

        String prompt = String.format("""
            你是诗词接龙裁判。
            规则：下一句诗的第一个字必须与上一句诗的最后一个字相同。
            
            上一句尾字：「%s」
            用户输入：「%s」
            已使用的诗句：%s
            
            请判断用户输入是否符合规则：
            1. 必须是真实存在的古诗词原句
            2. 第一个字必须是「%s」
            
            只输出一行JSON，不要有任何其他内容：
            {"valid":false,"correctStart":false,"isPoem":false,"message":"xxx"}
            
            message填写规则：
            - 不是真实诗句：写「❌ 输入内容不是古诗词原句」
            - 首字不对：写「⚠️ 诗句首字不是「%s」」
            - 重复：写「🔄 这句诗已经说过了」
            - 正确：写「✅ 正确」
            """, lastChar, userLine, String.join("、", usedLines), lastChar, lastChar);

        String response = callDeepSeek(prompt);
        System.out.println("DeepSeek 判断返回: " + response);

        return parseJudgeResponse(response, userLine, lastChar);
    }

    public ChainResponse aiTurn(String lastChar, List<String> usedLines) {
        String prompt = String.format("""
            你是诗词接龙高手。
            规则：下一句诗的第一个字必须与上一句诗的最后一个字相同。
            
            请接的字：「%s」
            已使用的诗句：%s
            
            要求：
            1. 输出一句真实存在的古诗词原句
            2. 第一个字必须是「%s」
            3. 不能与已使用的诗句重复
            4. 如果想不出来，只输出「认输」
            5. 只输出诗句本身，不要有任何解释
            """, lastChar, String.join("、", usedLines), lastChar);

        String response = callDeepSeek(prompt);
        System.out.println("DeepSeek AI接龙返回: " + response);

        String line = response.trim()
                .replaceAll("[\"']", "")
                .replaceAll("^[一二三四五、]*", "");

        if (line.contains("认输") || line.isEmpty()) {
            return new ChainResponse(false, "我想不出来了... 认输！", null, null, null);
        }

        // 检查是否重复
        if (usedLines.contains(line)) {
            // 重试一次
            return aiTurn(lastChar, usedLines);
        }

        String nextChar = getLastChar(line);
        return new ChainResponse(true, "AI接：「" + line + "」", nextChar, line, null);
    }

    private String getLastChar(String line) {
        if (line == null || line.isEmpty()) {
            return "";
        }
        // 去掉标点符号，获取最后一个汉字
        String cleanLine = line.replaceAll("[，。！？；：、]", "");
        if (cleanLine.isEmpty()) {
            return "";
        }
        return String.valueOf(cleanLine.charAt(cleanLine.length() - 1));
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

    private ChainResponse parseJudgeResponse(String response, String userLine, String expectedStartChar) {
        try {
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
            boolean correctStart = json.has("correctStart") && json.path("correctStart").asBoolean();
            boolean isPoem = json.has("isPoem") && json.path("isPoem").asBoolean();

            String message = json.has("message") ? json.path("message").asText() : "";
            if (message.isEmpty() || message.equals("判断完成")) {
                if (!isPoem) {
                    message = "❌ \"" + userLine + "\" 不是古诗词原句";
                } else if (!correctStart) {
                    String actualStart = userLine.isEmpty() ? "" : String.valueOf(userLine.charAt(0));
                    message = "⚠️ 诗句首字「" + actualStart + "」不是「" + expectedStartChar + "」";
                } else {
                    message = "✅ 正确！";
                }
            }

            if (valid && correctStart && isPoem) {
                String nextChar = getLastChar(userLine);
                return new ChainResponse(true, message, nextChar, null, null);
            } else {
                return new ChainResponse(false, message, null, null, null);
            }

        } catch (Exception e) {
            e.printStackTrace();
            return new ChainResponse(false, "裁判判断失败，请重试", null, null, null);
        }
    }
}
