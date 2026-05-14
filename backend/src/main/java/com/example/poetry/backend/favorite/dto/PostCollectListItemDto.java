package com.example.poetry.backend.favorite.dto;

public record PostCollectListItemDto(
    long postId,
    String title,
    String topicTag,
    String contentPreview,
    String authorNickname,
    String collectedAt
) {
}
