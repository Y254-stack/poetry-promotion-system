package com.example.poetry.backend.ai.dto;

public record ChatRequest(
    String message,
    String conversationId
) {}
