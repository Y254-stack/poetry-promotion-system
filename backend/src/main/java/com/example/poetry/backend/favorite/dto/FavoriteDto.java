package com.example.poetry.backend.favorite.dto;

public record FavoriteDto(
    Long favoriteId,
    Long userId,
    Long workId,
    String createdAt
) {
}
