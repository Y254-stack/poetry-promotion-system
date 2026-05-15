package com.example.poetry.backend.recommendation.dto;

public record DailyRecommendationMetaDto(
    String recommendDate,
    String themeName,
    String introText
) {
}
