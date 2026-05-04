package com.example.poetry.backend.user.dto;

public record UserProfileResponse(
    Long userId,
    String username,
    String nickname,
    String email
) {
}
