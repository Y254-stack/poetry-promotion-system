package com.example.poetry.backend.ai.dto;

public record ChatResponse(
    String message,
    String conversationId,
    long timestamp
) {}
