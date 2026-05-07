package com.example.poetry.backend.community.dto;

import java.time.LocalDateTime;

public record PostDetailResponse(
        Long postId,
        Long userId,
        String author,
        String title,
        String contentText,
        String topicTag,
        int viewCount,
        int likeCount,
        int commentCount,
        int collectCount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}