package com.example.poetry.backend.community.dto;

public record CollectResponse(
        boolean success,
        boolean isCollected,
        int collectCount,
        String message
) {
}