package com.example.poetry.backend.community.service;

import com.example.poetry.backend.community.dto.NotificationListResponse;
import com.example.poetry.backend.community.dto.NotificationResponse;
import com.example.poetry.backend.community.repository.NotificationRepository;
import com.example.poetry.backend.community.repository.PostRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final PostRepository postRepository;

    public NotificationService(NotificationRepository notificationRepository,
                               PostRepository postRepository) {
        this.notificationRepository = notificationRepository;
        this.postRepository = postRepository;
    }

    /**
     * 获取用户通知列表
     */
    public NotificationListResponse getNotifications(Long userId, int page, int pageSize) {
        if (page < 1) page = 1;
        if (pageSize < 1) pageSize = 20;
        if (pageSize > 100) pageSize = 100;

        int offset = (page - 1) * pageSize;
        var items = notificationRepository.getNotifications(userId, offset, pageSize);
        long total = notificationRepository.getNotificationCount(userId);
        long unreadCount = notificationRepository.getUnreadCount(userId);
        boolean hasMore = (long) offset + pageSize < total;

        return new NotificationListResponse(items, page, pageSize, total, hasMore, unreadCount);
    }

    /**
     * 标记单条通知为已读
     */
    public boolean markAsRead(Long notificationId, Long userId) {
        return notificationRepository.markAsRead(notificationId, userId);
    }

    /**
     * 标记所有通知为已读
     */
    public boolean markAllAsRead(Long userId) {
        return notificationRepository.markAllAsRead(userId);
    }

    /**
     * 删除单条通知
     */
    public boolean deleteNotification(Long notificationId, Long userId) {
        return notificationRepository.deleteNotification(notificationId, userId);
    }

    /**
     * 删除所有通知
     */
    public boolean deleteAllNotifications(Long userId) {
        return notificationRepository.deleteAllNotifications(userId);
    }

    /**
     * 获取未读通知数量
     */
    public long getUnreadCount(Long userId) {
        return notificationRepository.getUnreadCount(userId);
    }

    /**
     * 创建通知（供其他服务调用）
     */
    public void createNotification(Long userId, String type, Long actorId, Long postId, Long commentId) {
        // 验证帖子是否存在
        postRepository.getPostDetail(postId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "帖子不存在"));

        // 不给自己发通知
        if (userId.equals(actorId)) {
            return;
        }

        notificationRepository.createNotification(userId, type, actorId, postId, commentId);
    }
}