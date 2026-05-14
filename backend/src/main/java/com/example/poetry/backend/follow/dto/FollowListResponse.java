package com.example.poetry.backend.follow.dto;

import java.util.List;

public record FollowListResponse(
    int page,
    int pageSize,
    int total,
    List<FollowListItemDto> items
) {
}
