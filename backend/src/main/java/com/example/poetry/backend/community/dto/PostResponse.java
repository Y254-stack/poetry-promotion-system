package com.example.poetry.backend.community.dto;

import java.time.LocalDateTime;

public record PostResponse(
        Long postId,
        Long userId,
        String author,      // 作者昵称
        String title,
        String preview,     // 内容预览（前50字）
        String topicTag,
        int viewCount,
        int likeCount,
        int commentCount,
        int collectCount,
        LocalDateTime createdAt
) {
}
