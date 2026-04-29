package com.example.poetry.backend.user.dto;

public record AuthResponse(
    String token,
    Long userId,
    String username,
    String nickname
) {
}
