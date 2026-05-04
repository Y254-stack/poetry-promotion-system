package com.example.poetry.backend.tagsearch.dto;

import java.util.List;

public record PoemTitleSearchResponse(
    String query,
    int page,
    int pageSize,
    int total,
    List<PoemSearchItemDto> items,
    String emptyMessage
) {
}
