package com.example.poetry.backend.user.repository;

import java.time.LocalDateTime;

public record UserAccount(
    Long userId,
    String username,
    String passwordHash,
    String nickname,
    String email,
    LocalDateTime loginLockedUntil,
    int failedLoginCount,
    LocalDateTime failedLoginWindowStart
) {
}
