package com.example.poetry.backend.community.service;

import com.example.poetry.backend.community.dto.NotificationListResponse;
import com.example.poetry.backend.community.dto.NotificationResponse;
import com.example.poetry.backend.community.repository.NotificationRepository;
import com.example.poetry.backend.community.repository.PostRepository;
import com.example.poetry.backend.community.dto.PostDetailResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private PostRepository postRepository;

    @InjectMocks
    private NotificationService notificationService;

    // 辅助方法：创建测试用的 NotificationResponse
    private NotificationResponse createMockNotification(Long id, Long userId, String type,
                                                       Long actorId, Long postId, String actorName) {
        return new NotificationResponse(
                id, userId, type, actorId, postId, null, actorName,
                "测试帖子标题", null, LocalDateTime.now(), false
        );
    }

    // ==================== 通知列表测试 ====================

    @Test
    void getNotifications_正常分页_返回列表() {
        Long userId = 100L;
        List<NotificationResponse> mockNotifications = List.of(
                createMockNotification(1L, userId, "LIKE", 200L, 1L, "用户A"),
                createMockNotification(2L, userId, "COMMENT", 201L, 2L, "用户B")
        );

        when(notificationRepository.getNotifications(userId, 0, 20)).thenReturn(mockNotifications);
        when(notificationRepository.getNotificationCount(userId)).thenReturn(2L);
        when(notificationRepository.getUnreadCount(userId)).thenReturn(1L);

        NotificationListResponse response = notificationService.getNotifications(userId, 1, 20);

        assertThat(response.items()).hasSize(2);
        assertThat(response.page()).isEqualTo(1);
        assertThat(response.pageSize()).isEqualTo(20);
        assertThat(response.total()).isEqualTo(2);
        assertThat(response.hasMore()).isFalse();
        assertThat(response.unreadCount()).isEqualTo(1);
    }

    @Test
    void getNotifications_有更多数据_hasMore为true() {
        when(notificationRepository.getNotifications(anyLong(), eq(0), eq(20))).thenReturn(List.of());
        when(notificationRepository.getNotificationCount(anyLong())).thenReturn(25L);
        when(notificationRepository.getUnreadCount(anyLong())).thenReturn(0L);

        NotificationListResponse response = notificationService.getNotifications(100L, 1, 20);

        assertThat(response.hasMore()).isTrue();
    }

    @Test
    void getNotifications_参数边界处理_page为0设为1() {
        when(notificationRepository.getNotifications(anyLong(), eq(0), eq(20))).thenReturn(List.of());
        when(notificationRepository.getNotificationCount(anyLong())).thenReturn(0L);
        when(notificationRepository.getUnreadCount(anyLong())).thenReturn(0L);

        notificationService.getNotifications(100L, 0, 20);

        verify(notificationRepository).getNotifications(anyLong(), eq(0), eq(20));
    }

    @Test
    void getNotifications_参数边界处理_pageSize为0设为20() {
        when(notificationRepository.getNotifications(anyLong(), eq(0), eq(20))).thenReturn(List.of());
        when(notificationRepository.getNotificationCount(anyLong())).thenReturn(0L);
        when(notificationRepository.getUnreadCount(anyLong())).thenReturn(0L);

        notificationService.getNotifications(100L, 1, 0);

        verify(notificationRepository).getNotifications(anyLong(), eq(0), eq(20));
    }

    @Test
    void getNotifications_参数边界处理_pageSize超过100设为100() {
        when(notificationRepository.getNotifications(anyLong(), eq(0), eq(100))).thenReturn(List.of());
        when(notificationRepository.getNotificationCount(anyLong())).thenReturn(0L);
        when(notificationRepository.getUnreadCount(anyLong())).thenReturn(0L);

        notificationService.getNotifications(100L, 1, 200);

        verify(notificationRepository).getNotifications(anyLong(), eq(0), eq(100));
    }

    // ==================== 标记已读测试 ====================

    @Test
    void markAsRead_成功_返回true() {
        when(notificationRepository.markAsRead(1L, 100L)).thenReturn(true);

        boolean result = notificationService.markAsRead(1L, 100L);

        assertThat(result).isTrue();
    }

    @Test
    void markAsRead_通知不存在_返回false() {
        when(notificationRepository.markAsRead(999L, 100L)).thenReturn(false);

        boolean result = notificationService.markAsRead(999L, 100L);

        assertThat(result).isFalse();
    }

    @Test
    void markAllAsRead_成功_返回true() {
        when(notificationRepository.markAllAsRead(100L)).thenReturn(true);

        boolean result = notificationService.markAllAsRead(100L);

        assertThat(result).isTrue();
    }

    // ==================== 删除通知测试 ====================

    @Test
    void deleteNotification_成功_返回true() {
        when(notificationRepository.deleteNotification(1L, 100L)).thenReturn(true);

        boolean result = notificationService.deleteNotification(1L, 100L);

        assertThat(result).isTrue();
    }

    @Test
    void deleteNotification_无权删除_返回false() {
        when(notificationRepository.deleteNotification(1L, 200L)).thenReturn(false);

        boolean result = notificationService.deleteNotification(1L, 200L);

        assertThat(result).isFalse();
    }

    @Test
    void deleteAllNotifications_成功_返回true() {
        when(notificationRepository.deleteAllNotifications(100L)).thenReturn(true);

        boolean result = notificationService.deleteAllNotifications(100L);

        assertThat(result).isTrue();
    }

    // ==================== 未读数量测试 ====================

    @Test
    void getUnreadCount_成功_返回数量() {
        when(notificationRepository.getUnreadCount(100L)).thenReturn(5L);

        long count = notificationService.getUnreadCount(100L);

        assertThat(count).isEqualTo(5);
    }

    // ==================== 创建通知测试 ====================

    @Test
    void createNotification_正常情况_创建成功() {
        PostDetailResponse mockPost = new PostDetailResponse(
                1L, 200L, "作者", "标题", "内容", "标签",
                10, 5, 3, 2, LocalDateTime.now(), LocalDateTime.now(), false, false
        );

        when(postRepository.getPostDetail(1L)).thenReturn(Optional.of(mockPost));
        doNothing().when(notificationRepository).createNotification(eq(200L), eq("LIKE"), eq(100L), eq(1L), eq(null));

        notificationService.createNotification(200L, "LIKE", 100L, 1L, null);

        verify(notificationRepository).createNotification(eq(200L), eq("LIKE"), eq(100L), eq(1L), eq(null));
    }

    @Test
    void createNotification_帖子不存在_抛出异常() {
        when(postRepository.getPostDetail(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> notificationService.createNotification(200L, "LIKE", 100L, 999L, null))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("帖子不存在");
    }

    @Test
    void createNotification_给自己发通知_不创建() {
        PostDetailResponse mockPost = new PostDetailResponse(
                1L, 100L, "作者", "标题", "内容", "标签",
                10, 5, 3, 2, LocalDateTime.now(), LocalDateTime.now(), false, false
        );

        when(postRepository.getPostDetail(1L)).thenReturn(Optional.of(mockPost));

        notificationService.createNotification(100L, "LIKE", 100L, 1L, null);

        verify(notificationRepository, never()).createNotification(anyLong(), anyString(), anyLong(), anyLong(), any());
    }
}