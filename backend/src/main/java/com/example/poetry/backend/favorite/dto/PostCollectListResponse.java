package com.example.poetry.backend.favorite.dto;

import java.util.List;

public record PostCollectListResponse(
    int page,
    int pageSize,
    int total,
    List<PostCollectListItemDto> items
) {
}
