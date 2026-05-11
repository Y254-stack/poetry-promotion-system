package com.example.poetry.backend.community.dto;

public record LikeResponse(
        boolean success,
        boolean isLiked,
        int likeCount,
        String message
) {
}
