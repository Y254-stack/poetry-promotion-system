package com.example.poetry.backend.ai.controller;

import com.example.poetry.backend.ai.dto.ChatRequest;
import com.example.poetry.backend.ai.dto.ChatResponse;
import com.example.poetry.backend.ai.service.ClaudeService;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/ai")
public class AiChatController {

    private final ClaudeService claudeService;

    // Simple in-memory conversation storage (for demo purposes)
    private final Map<String, List<Map<String, String>>> conversations = new ConcurrentHashMap<>();

    public AiChatController(ClaudeService claudeService) {
        this.claudeService = claudeService;
    }

    @PostMapping("/chat")
    public ChatResponse chat(@RequestBody ChatRequest request) {
        String conversationId = request.conversationId() != null ? request.conversationId() : generateConversationId();

        // Get or create conversation history
        List<Map<String, String>> history = conversations.computeIfAbsent(conversationId, k -> new ArrayList<>());

        // Call Claude API
        String aiResponse = claudeService.chat(request.message(), history);

        // Update conversation history
        Map<String, String> userMsg = new HashMap<>();
        userMsg.put("role", "user");
        userMsg.put("content", request.message());
        history.add(userMsg);

        Map<String, String> assistantMsg = new HashMap<>();
        assistantMsg.put("role", "assistant");
        assistantMsg.put("content", aiResponse);
        history.add(assistantMsg);

        return new ChatResponse(aiResponse, conversationId, System.currentTimeMillis());
    }

    private String generateConversationId() {
        return "conv_" + System.currentTimeMillis() + "_" + (int)(Math.random() * 10000);
    }
}
