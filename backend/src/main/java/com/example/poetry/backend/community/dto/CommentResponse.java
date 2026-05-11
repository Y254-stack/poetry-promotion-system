package com.example.poetry.backend.community.dto;

import java.time.LocalDateTime;

public record CommentResponse(
        Long commentId,
        Long postId,
        Long userId,
        String author,
        String contentText,
        Long parentCommentId,
        Long replyUserId,
        String replyToAuthor,  // 被回复者的昵称
        LocalDateTime createdAt,
        Integer likeCount,     // 点赞数
        Boolean isLiked        // 当前用户是否已点赞
) {
    // 兼容旧版本的构造函数
    public CommentResponse(Long commentId, Long postId, Long userId, String author,
                           String contentText, Long parentCommentId, Long replyUserId,
                           String replyToAuthor, LocalDateTime createdAt) {
        this(commentId, postId, userId, author, contentText, parentCommentId,
                replyUserId, replyToAuthor, createdAt, 0, false);
    }
}