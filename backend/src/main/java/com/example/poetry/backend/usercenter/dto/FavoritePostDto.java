package com.example.poetry.backend.usercenter.dto;

public record FavoritePostDto(
    Long postId,
    String title,
    String contentPreview,
    String topicTag,
    Integer collectCount,
    Long authorUserId,
    String authorNickname,
    String collectedAt
) {
}

