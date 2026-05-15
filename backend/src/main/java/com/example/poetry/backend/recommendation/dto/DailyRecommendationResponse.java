package com.example.poetry.backend.recommendation.dto;

import java.util.List;

public record DailyRecommendationResponse(
    String recommendDate,
    String themeName,
    String introText,
    int offset,
    int totalDays,
    List<DailyRecommendationItemDto> items
) {
}
