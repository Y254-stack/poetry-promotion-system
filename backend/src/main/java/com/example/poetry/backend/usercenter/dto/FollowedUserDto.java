package com.example.poetry.backend.usercenter.dto;

public record FollowedUserDto(
    Long userId,
    String username,
    String nickname,
    String email,
    String followedAt
) {
}

