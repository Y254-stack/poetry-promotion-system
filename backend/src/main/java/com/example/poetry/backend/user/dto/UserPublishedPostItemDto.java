package com.example.poetry.backend.user.dto;

public record UserPublishedPostItemDto(
    long postId,
    String title,
    String topicTag,
    String contentPreview,
    String publishedAt
) {
}
