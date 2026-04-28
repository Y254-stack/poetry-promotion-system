package com.example.poetry.backend.tagsearch.dto;

import java.util.List;

public record PoemTitleSearchResponse(
    String query,
    Integer page,
    Integer pageSize,
    Integer total,
    List<PoemSearchItemDto> items,
    String emptyMessage
) {
}
