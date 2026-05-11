package com.example.poetry.backend.ai.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ClaudeService {

    @Value("${anthropic.api.key}")
    private String apiKey;

    private static final String CLAUDE_API_URL = "https://api.anthropic.com/v1/messages";
    private static final String MODEL = "claude-sonnet-4-6";

    private final RestTemplate restTemplate;

    public ClaudeService() {
        this.restTemplate = new RestTemplate();
    }

    public String chat(String userMessage, List<Map<String, String>> conversationHistory) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-api-key", apiKey);
        headers.set("anthropic-version", "2023-06-01");

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", MODEL);
        requestBody.put("max_tokens", 1024);

        // System prompt for poetry assistant
        requestBody.put("system", "你是一个专业的古诗词助手，精通中国古典诗词。你可以帮助用户理解诗词的含义、背景、作者信息，以及诗词的艺术特色。请用简洁、易懂的语言回答用户的问题。");

        // Build messages array
        List<Map<String, String>> messages = new java.util.ArrayList<>(conversationHistory);
        Map<String, String> userMsg = new HashMap<>();
        userMsg.put("role", "user");
        userMsg.put("content", userMessage);
        messages.add(userMsg);

        requestBody.put("messages", messages);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

        try {
            Map<String, Object> response = restTemplate.postForObject(CLAUDE_API_URL, request, Map.class);

            if (response != null && response.containsKey("content")) {
                List<Map<String, Object>> content = (List<Map<String, Object>>) response.get("content");
                if (!content.isEmpty()) {
                    return (String) content.get(0).get("text");
                }
            }

            return "抱歉，我现在无法回答。";
        } catch (Exception e) {
            throw new RuntimeException("调用 Claude API 失败: " + e.getMessage(), e);
        }
    }
}
