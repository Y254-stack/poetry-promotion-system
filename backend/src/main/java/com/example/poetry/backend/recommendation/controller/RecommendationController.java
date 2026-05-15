package com.example.poetry.backend.recommendation.controller;

import com.example.poetry.backend.recommendation.dto.DailyRecommendationResponse;
import com.example.poetry.backend.recommendation.dto.RelatedWorkResponse;
import com.example.poetry.backend.recommendation.service.RecommendationService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/recommendations")
public class RecommendationController {

    private final RecommendationService recommendationService;

    public RecommendationController(RecommendationService recommendationService) {
        this.recommendationService = recommendationService;
    }

    @GetMapping("/daily")
    public DailyRecommendationResponse getDailyRecommendation(
        @RequestParam(defaultValue = "0") int offset,
        @RequestParam(defaultValue = "5") @Min(1) @Max(20) int limit
    ) {
        return recommendationService.getDailyRecommendation(offset, limit);
    }

    @GetMapping("/related")
    public RelatedWorkResponse getRelatedWorks(
        @RequestParam Long workId,
        @RequestParam(defaultValue = "6") @Min(1) @Max(20) int limit
    ) {
        return recommendationService.getRelatedWorks(workId, limit);
    }
}
