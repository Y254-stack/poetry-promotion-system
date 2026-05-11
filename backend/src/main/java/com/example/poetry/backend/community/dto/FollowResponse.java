package com.example.poetry.backend.community.dto;

public record FollowResponse(
        boolean success,
        boolean isFollowing,
        long followingCount,
        long followerCount,
        String message
) {
}
