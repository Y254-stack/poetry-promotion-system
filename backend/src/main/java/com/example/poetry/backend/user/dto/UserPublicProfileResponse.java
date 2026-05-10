package com.example.poetry.backend.user.dto;

public record UserPublicProfileResponse(
    long userId,
    String username,
    String nickname
) {
}
