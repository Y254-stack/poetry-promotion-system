package com.example.poetry.backend.community.controller;

import com.example.poetry.backend.community.dto.NotificationListResponse;
import com.example.poetry.backend.community.service.NotificationService;
import com.example.poetry.backend.user.security.JwtTokenProvider;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/community")
public class NotificationController {

    private final NotificationService notificationService;
    private final JwtTokenProvider jwtTokenProvider;

    public NotificationController(NotificationService notificationService,
                                   JwtTokenProvider jwtTokenProvider) {
        this.notificationService = notificationService;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    /**
     * 获取用户通知列表
     */
    @GetMapping("/notifications")
    public NotificationListResponse getNotifications(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize
    ) {
        Long userId = extractUserId(authorization);
        return notificationService.getNotifications(userId, page, pageSize);
    }

    /**
     * 获取未读通知数量
     */
    @GetMapping("/notifications/unread-count")
    public Map<String, Long> getUnreadCount(
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        Long userId = extractUserId(authorization);
        long count = notificationService.getNotifications(userId, 1, 1).unreadCount();
        Map<String, Long> result = new HashMap<>();
        result.put("count", count);
        return result;
    }

    /**
     * 标记单条通知为已读
     */
    @PostMapping("/notification/{notificationId}/read")
    public Map<String, Object> markAsRead(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable Long notificationId
    ) {
        Long userId = extractUserId(authorization);
        boolean success = notificationService.markAsRead(notificationId, userId);
        Map<String, Object> result = new HashMap<>();
        result.put("success", success);
        return result;
    }

    /**
     * 标记所有通知为已读
     */
    @PostMapping("/notifications/read-all")
    public Map<String, Object> markAllAsRead(
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        Long userId = extractUserId(authorization);
        boolean success = notificationService.markAllAsRead(userId);
        Map<String, Object> result = new HashMap<>();
        result.put("success", success);
        return result;
    }

    /**
     * 删除单条通知
     */
    @DeleteMapping("/notification/{notificationId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteNotification(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable Long notificationId
    ) {
        Long userId = extractUserId(authorization);
        boolean success = notificationService.deleteNotification(notificationId, userId);
        if (!success) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "通知不存在");
        }
    }

    /**
     * 删除所有通知
     */
    @DeleteMapping("/notifications")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAllNotifications(
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        Long userId = extractUserId(authorization);
        notificationService.deleteAllNotifications(userId);
    }

    private Long extractUserId(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "缺少 Bearer Token");
        }
        String token = authorization.substring("Bearer ".length()).trim();
        return jwtTokenProvider.parseUserId(token);
    }
}