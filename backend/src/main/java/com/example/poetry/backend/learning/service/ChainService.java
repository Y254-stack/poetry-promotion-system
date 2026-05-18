package com.example.poetry.backend.learning.service;

import com.example.poetry.backend.learning.dto.ChainResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class ChainService {

    private static final Logger log = LoggerFactory.getLogger(ChainService.class);

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String apiKey;
    private final String apiUrl;

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

    public ChainService(
            @Value("${deepseek.api.key}") String apiKey,
            @Value("${deepseek.api.url:https://api.deepseek.com/v1/chat/completions}") String apiUrl) {
        this.apiKey = apiKey;
        this.apiUrl = apiUrl;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();
    }

    public ChainResponse startGame() {
        String startLine = startLines.get(ThreadLocalRandom.current().nextInt(startLines.size()));
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
        log.debug("DeepSeek 判断返回: {}", response);

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
        log.debug("DeepSeek AI接龙返回: {}", response);

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
                log.warn("API调用失败，状态码: {}", response.statusCode());
                return "";
            }

            JsonNode root = objectMapper.readTree(response.body());
            JsonNode choices = root.path("choices");
            if (!choices.isArray() || choices.isEmpty()) {
                log.warn("API返回 choices 为空或格式异常");
                return "";
            }
            JsonNode firstChoice = choices.get(0);
            JsonNode messageNode = firstChoice.path("message");
            JsonNode contentNode = messageNode.path("content");
            return contentNode.asText();

        } catch (JsonProcessingException e) {
            log.error("JSON 解析失败", e);
            return "";
        } catch (IOException e) {
            log.error("API 调用网络异常", e);
            return "";
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("API 调用被中断", e);
            return "";
        }
    }

    private ChainResponse parseJudgeResponse(String response, String userLine, String expectedStartChar) {
        try {
            String cleanResponse = stripMarkdownFences(response.trim());
            log.debug("清理后的JSON: {}", cleanResponse);

            JsonNode json = objectMapper.readTree(cleanResponse);
            boolean valid = json.has("valid") && json.path("valid").asBoolean();
            boolean correctStart = json.has("correctStart") && json.path("correctStart").asBoolean();
            boolean isPoem = json.has("isPoem") && json.path("isPoem").asBoolean();
            String message = resolveMessage(json, userLine, expectedStartChar, isPoem, correctStart);

            if (valid && correctStart && isPoem) {
                return new ChainResponse(true, message, getLastChar(userLine), null, null);
            }
            return new ChainResponse(false, message, null, null, null);

        } catch (JsonProcessingException e) {
            log.error("裁判JSON解析失败", e);
            return new ChainResponse(false, "裁判判断失败，请重试", null, null, null);
        }
    }

    private static String stripMarkdownFences(String raw) {
        String result = raw;
        if (result.startsWith("```json")) {
            result = result.substring(7);
        }
        if (result.startsWith("```")) {
            result = result.substring(3);
        }
        if (result.endsWith("```")) {
            result = result.substring(0, result.length() - 3);
        }
        return result.trim();
    }

    private static String resolveMessage(JsonNode json, String userLine, String expectedStartChar,
                                         boolean isPoem, boolean correctStart) {
        String message = json.has("message") ? json.path("message").asText() : "";
        if (!message.isEmpty() && !message.equals("判断完成")) {
            return message;
        }
        if (!isPoem) {
            return "❌ \"" + userLine + "\" 不是古诗词原句";
        }
        if (!correctStart) {
            String actualStart = userLine.isEmpty() ? "" : String.valueOf(userLine.charAt(0));
            return "⚠️ 诗句首字「" + actualStart + "」不是「" + expectedStartChar + "」";
        }
        return "✅ 正确！";
    }
}
