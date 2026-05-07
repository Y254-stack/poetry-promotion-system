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
        LocalDateTime createdAt
) {
}