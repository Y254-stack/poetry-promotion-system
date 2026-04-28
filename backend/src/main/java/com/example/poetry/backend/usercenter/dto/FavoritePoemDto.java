package com.example.poetry.backend.usercenter.dto;

public record FavoritePoemDto(
    Long workId,
    String title,
    String authorName,
    String dynastyName,
    String contentPreview,
    String collectedAt
) {
}

