package com.example.poetry.backend.recommendation.service;

import com.example.poetry.backend.recommendation.dto.DailyRecommendationItemDto;
import com.example.poetry.backend.recommendation.dto.DailyRecommendationMetaDto;
import com.example.poetry.backend.recommendation.dto.DailyRecommendationResponse;
import com.example.poetry.backend.recommendation.dto.RelatedWorkResponse;
import com.example.poetry.backend.recommendation.repository.RecommendationRepository;
import com.example.poetry.backend.tagsearch.dto.PoemSearchItemDto;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class RecommendationService {

    private final RecommendationRepository recommendationRepository;

    public RecommendationService(RecommendationRepository recommendationRepository) {
        this.recommendationRepository = recommendationRepository;
    }

    public DailyRecommendationResponse getDailyRecommendation(int offset, int limit) {
        List<LocalDate> dates = recommendationRepository.findRecommendationDates();
        LocalDate today = LocalDate.now();
        if (dates.isEmpty()) {
            return new DailyRecommendationResponse(today.toString(), "", null, 0, 0, List.of());
        }

        int totalDays = dates.size();
        int normalizedOffset = Math.floorMod(offset, totalDays);
        int anchorIndex = findAnchorIndex(dates, today);
        LocalDate targetDate = dates.get((anchorIndex + normalizedOffset) % totalDays);

        DailyRecommendationMetaDto meta = recommendationRepository.findDailyRecommendationMeta(targetDate);
        List<DailyRecommendationItemDto> items =
            recommendationRepository.findDailyRecommendationItems(targetDate, limit);

        if (meta == null) {
            return new DailyRecommendationResponse(today.toString(), "", null, normalizedOffset, totalDays, items);
        }

        return new DailyRecommendationResponse(
            today.toString(),
            meta.themeName(),
            meta.introText(),
            normalizedOffset,
            totalDays,
            items
        );
    }

    public RelatedWorkResponse getRelatedWorks(Long workId, int limit) {
        List<PoemSearchItemDto> items = recommendationRepository.findRelatedWorks(workId, limit);
        return new RelatedWorkResponse(workId, limit, items);
    }

    private int findAnchorIndex(List<LocalDate> dates, LocalDate today) {
        for (int index = 0; index < dates.size(); index++) {
            if (!dates.get(index).isBefore(today)) {
                return index;
            }
        }
        return dates.size() - 1;
    }
}
