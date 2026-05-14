package com.example.poetry.backend.follow.dto;

public record FollowListItemDto(
    long userId,
    String username,
    String nickname,
    String followedAt
) {
}
