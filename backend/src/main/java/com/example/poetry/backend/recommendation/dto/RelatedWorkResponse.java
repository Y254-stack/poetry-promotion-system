package com.example.poetry.backend.recommendation.dto;

import com.example.poetry.backend.tagsearch.dto.PoemSearchItemDto;
import java.util.List;

public record RelatedWorkResponse(
    Long workId,
    int limit,
    List<PoemSearchItemDto> items
) {
}
