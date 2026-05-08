package com.example.poetry.backend.community.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CommentCreateRequest(
        @NotNull(message = "帖子ID不能为空")
        Long postId,

        @NotBlank(message = "评论内容不能为空")
        @Size(max = 1000, message = "评论内容最多1000个字符")
        String contentText,

        Long parentCommentId,  // 回复的评论ID，一级评论为null

        Long replyUserId       // 被回复的用户ID，一级评论为null
) {
}