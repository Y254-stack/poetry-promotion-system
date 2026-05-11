package com.example.poetry.backend.community.dto;

import java.util.List;

public record FollowingListResponse(
        List<UserBriefInfo> items,
        int page,
        int pageSize,
        long total,
        boolean hasMore
) {
    public record UserBriefInfo(
            Long userId,
            String nickname,
            String avatarUrl,
            String bio
    ) {
    }
}
