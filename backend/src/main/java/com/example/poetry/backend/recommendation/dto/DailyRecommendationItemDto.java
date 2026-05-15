package com.example.poetry.backend.recommendation.dto;

public record DailyRecommendationItemDto(
    Long workId,
    Long authorId,
    String title,
    String authorName,
    String dynastyName,
    String reasonType,
    String reasonText
) {
}
