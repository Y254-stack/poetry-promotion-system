package com.example.poetry.backend.community.dto;

import java.time.LocalDateTime;

public record NotificationResponse(
        Long notificationId,
        Long userId,
        String type,
        Long actorId,
        String actorName,
        Long postId,
        String postTitle,
        Long commentId,
        String commentContent,
        boolean isRead,
        LocalDateTime createdAt
) {
}