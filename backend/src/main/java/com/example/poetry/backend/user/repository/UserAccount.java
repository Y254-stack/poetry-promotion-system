package com.example.poetry.backend.user.repository;

public record UserAccount(
    Long userId,
    String username,
    String passwordHash,
    String nickname,
    String email
) {
}
