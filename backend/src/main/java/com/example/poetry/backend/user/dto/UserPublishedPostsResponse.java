package com.example.poetry.backend.user.dto;

import java.util.List;

public record UserPublishedPostsResponse(
    int page,
    int pageSize,
    int total,
    List<UserPublishedPostItemDto> items
) {
}
