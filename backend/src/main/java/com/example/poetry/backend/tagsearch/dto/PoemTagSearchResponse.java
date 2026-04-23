package com.example.poetry.backend.tagsearch.dto;

import java.util.List;

public record PoemTagSearchResponse(
    List<Long> selectedTagIds,
    String sort,
    Integer page,
    Integer pageSize,
    Integer total,
    Boolean hasMore,
    List<PoemSearchItemDto> items,
    String emptyMessage,
    List<TagDto> recommendedTags
) {
}

