package com.example.poetry.backend.community.dto;

import java.time.LocalDateTime;

public record UserPublicProfile(
        Long userId,
        String username,
        String nickname,
        String avatarUrl,
        String bio,
        String email,
        long likeCount,
        long followingCount,
        long followerCount,
        boolean isFollowing,
        LocalDateTime createdAt
) {
}
