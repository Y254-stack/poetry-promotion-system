package com.example.poetry.backend.tagsearch.dto;

import java.util.List;

public record PoemTagSearchResponse(
    List<Long> tagIds,
    String sort,
    int page,
    int pageSize,
    int total,
    boolean hasMore,
    List<PoemSearchItemDto> items,
    String emptyMessage,
    List<TagDto> recommendedTags
) {
}
