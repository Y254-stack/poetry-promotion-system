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
        LocalDateTime updatedAt,
        boolean isLiked,
        boolean isCollected
) {
    // 兼容旧版本的构造函数（不含 isLiked 和 isCollected）
    public PostDetailResponse(Long postId, Long userId, String author, String title,
                              String contentText, String topicTag, int viewCount, int likeCount,
                              int commentCount, int collectCount, LocalDateTime createdAt,
                              LocalDateTime updatedAt) {
        this(postId, userId, author, title, contentText, topicTag, viewCount, likeCount,
             commentCount, collectCount, createdAt, updatedAt, false, false);
    }
}
