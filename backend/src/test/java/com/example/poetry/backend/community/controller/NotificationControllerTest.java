package com.example.poetry.backend.community.controller;

import com.example.poetry.backend.community.dto.NotificationListResponse;
import com.example.poetry.backend.community.dto.NotificationResponse;
import com.example.poetry.backend.community.service.NotificationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(NotificationController.class)
class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private NotificationService notificationService;

    // ==================== 通知列表测试 ====================

    @Test
    void getNotifications_成功_返回通知列表() throws Exception {
        List<NotificationResponse> mockNotifications = List.of(
                new NotificationResponse(1L, 100L, "LIKE", 200L, 1L, null, "用户A", "标题1", null, LocalDateTime.now(), false),
                new NotificationResponse(2L, 100L, "COMMENT", 201L, 2L, 5L, "用户B", "标题2", "评论内容", LocalDateTime.now(), false)
        );
        NotificationListResponse response = new NotificationListResponse(mockNotifications, 1, 20, 2, false, 2);

        when(notificationService.getNotifications(eq(100L), eq(1), eq(20))).thenReturn(response);

        mockMvc.perform(get("/api/notifications")
                        .param("userId", "100")
                        .param("page", "1")
                        .param("pageSize", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.total").value(2))
                .andExpect(jsonPath("$.unreadCount").value(2));
    }

    // ==================== 标记已读测试 ====================

    @Test
    void markAsRead_成功_返回成功() throws Exception {
        when(notificationService.markAsRead(1L, 100L)).thenReturn(true);

        mockMvc.perform(post("/api/notifications/1/read")
                        .param("userId", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void markAllAsRead_成功_返回成功() throws Exception {
        when(notificationService.markAllAsRead(100L)).thenReturn(true);

        mockMvc.perform(post("/api/notifications/all-read")
                        .param("userId", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    // ==================== 删除通知测试 ====================

    @Test
    void deleteNotification_成功_返回成功() throws Exception {
        when(notificationService.deleteNotification(1L, 100L)).thenReturn(true);

        mockMvc.perform(delete("/api/notifications/1")
                        .param("userId", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void deleteAllNotifications_成功_返回成功() throws Exception {
        when(notificationService.deleteAllNotifications(100L)).thenReturn(true);

        mockMvc.perform(delete("/api/notifications/all")
                        .param("userId", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    // ==================== 获取未读数量测试 ====================

    @Test
    void getUnreadCount_成功_返回数量() throws Exception {
        when(notificationService.getUnreadCount(100L)).thenReturn(5L);

        mockMvc.perform(get("/api/notifications/unread-count")
                        .param("userId", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(5));
    }
}