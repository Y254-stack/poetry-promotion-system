package com.example.poetry.backend.community.dto;

import java.util.List;

public record NotificationListResponse(
        List<NotificationResponse> items,
        int page,
        int pageSize,
        long total,
        boolean hasMore,
        long unreadCount
) {
}