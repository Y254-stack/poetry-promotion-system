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
        LocalDateTime createdAt,
        boolean isFollowing  // 是否关注该作者
) {
    // 兼容旧版本的构造函数
    public PostResponse(Long postId, Long userId, String author, String title, 
                        String preview, String topicTag, int viewCount, int likeCount, 
                        int commentCount, int collectCount, LocalDateTime createdAt) {
        this(postId, userId, author, title, preview, topicTag, viewCount, likeCount, 
             commentCount, collectCount, createdAt, false);
    }
}
