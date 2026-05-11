package com.example.poetry.backend.ai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class DoubaoService {

    @Value("${anthropic.api.key}")
    private String apiKey;

    private static final String DOUBAO_API_URL = "https://ark.cn-beijing.volces.com/api/v3/chat/completions";
    private static final String MODEL = "ep-20260511193047-85rld";

    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;

    public DoubaoService() {
        this.httpClient = new OkHttpClient();
        this.objectMapper = new ObjectMapper();
    }

    public String chat(String userMessage, List<Map<String, String>> conversationHistory) {
        try {
            // Build messages array
            List<Map<String, String>> messages = new ArrayList<>();

            // Add system message
            Map<String, String> systemMsg = new HashMap<>();
            systemMsg.put("role", "system");
            systemMsg.put("content", "你是一个专业的古诗词助手，精通中国古典诗词。你可以帮助用户理解诗词的含义、背景、作者信息，以及诗词的艺术特色。请用简洁、易懂的语言回答用户的问题。");
            messages.add(systemMsg);

            // Add conversation history
            messages.addAll(conversationHistory);

            // Add current user message
            Map<String, String> userMsg = new HashMap<>();
            userMsg.put("role", "user");
            userMsg.put("content", userMessage);
            messages.add(userMsg);

            // Build request body
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", MODEL);
            requestBody.put("messages", messages);

            String jsonBody = objectMapper.writeValueAsString(requestBody);

            Request request = new Request.Builder()
                    .url(DOUBAO_API_URL)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Authorization", "Bearer " + apiKey)
                    .post(RequestBody.create(jsonBody, MediaType.parse("application/json")))
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    throw new IOException("豆包 API 调用失败: " + response.code() + " - " + response.message());
                }

                String responseBody = response.body().string();
                Map<String, Object> responseMap = objectMapper.readValue(responseBody, Map.class);

                List<Map<String, Object>> choices = (List<Map<String, Object>>) responseMap.get("choices");
                if (choices != null && !choices.isEmpty()) {
                    Map<String, Object> firstChoice = choices.get(0);
                    Map<String, Object> message = (Map<String, Object>) firstChoice.get("message");
                    return (String) message.get("content");
                }

                return "抱歉，我现在无法回答。";
            }
        } catch (Exception e) {
            throw new RuntimeException("调用豆包 API 失败: " + e.getMessage(), e);
        }
    }
}
