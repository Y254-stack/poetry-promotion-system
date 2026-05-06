package com.example.poetry.backend.favorite.dto;

import com.example.poetry.backend.tagsearch.dto.PoemSearchItemDto;
import java.util.List;

public record FavoriteListResponse(
    int page,
    int pageSize,
    int total,
    List<PoemSearchItemDto> items
) {
}
