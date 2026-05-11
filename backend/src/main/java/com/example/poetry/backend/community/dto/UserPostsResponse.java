package com.example.poetry.backend.community.dto;

import java.util.List;

public record UserPostsResponse(
        List<PostResponse> items,
        int page,
        int pageSize,
        long total,
        boolean hasMore
) {
}
