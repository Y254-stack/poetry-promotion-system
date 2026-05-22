package com.example.poetry.backend.community.service;

import com.example.poetry.backend.community.dto.UserPostsResponse;
import com.example.poetry.backend.community.dto.UserPublicProfile;
import com.example.poetry.backend.community.dto.PostResponse;
import com.example.poetry.backend.community.repository.FollowRepository;
import com.example.poetry.backend.community.repository.UserProfileRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserProfileServiceTest {

    @Mock
    private UserProfileRepository userProfileRepository;

    @Mock
    private FollowRepository followRepository;

    @InjectMocks
    private UserProfileService userProfileService;

    // ==================== 用户资料测试 ====================

    @Test
    void getUserProfile_用户存在_返回资料() {
        Long userId = 100L;
        Map<String, Object> profileMap = Map.of(
                "user_id", userId,
                "username", "testuser",
                "nickname", "测试用户",
                "avatar_url", "avatar.jpg",
                "bio", "这是我的简介",
                "email", "test@example.com",
                "created_at", "2024-01-01T10:00:00"
        );

        when(userProfileRepository.getUserProfile(userId)).thenReturn(Optional.of(profileMap));
        when(userProfileRepository.getUserLikeCount(userId)).thenReturn(15L);
        when(followRepository.getFollowingCount(userId)).thenReturn(20L);
        when(followRepository.getFollowerCount(userId)).thenReturn(50L);
        when(followRepository.isFollowing(200L, userId)).thenReturn(false);

        UserPublicProfile response = userProfileService.getUserProfile(userId, 200L);

        assertThat(response.userId()).isEqualTo(userId);
        assertThat(response.username()).isEqualTo("testuser");
        assertThat(response.nickname()).isEqualTo("测试用户");
        assertThat(response.avatarUrl()).isEqualTo("avatar.jpg");
        assertThat(response.bio()).isEqualTo("这是我的简介");
        assertThat(response.email()).isEqualTo("test@example.com");
        assertThat(response.likeCount()).isEqualTo(15);
        assertThat(response.followingCount()).isEqualTo(20);
        assertThat(response.followerCount()).isEqualTo(50);
        assertThat(response.isFollowing()).isFalse();
    }

    @Test
    void getUserProfile_当前用户关注该用户_isFollowing为true() {
        Long userId = 100L;
        Map<String, Object> profileMap = Map.of(
                "user_id", userId,
                "username", "testuser",
                "nickname", "测试用户",
                "avatar_url", null,
                "bio", null,
                "email", null,
                "created_at", null
        );

        when(userProfileRepository.getUserProfile(userId)).thenReturn(Optional.of(profileMap));
        when(userProfileRepository.getUserLikeCount(userId)).thenReturn(0L);
        when(followRepository.getFollowingCount(userId)).thenReturn(0L);
        when(followRepository.getFollowerCount(userId)).thenReturn(0L);
        when(followRepository.isFollowing(200L, userId)).thenReturn(true);

        UserPublicProfile response = userProfileService.getUserProfile(userId, 200L);

        assertThat(response.isFollowing()).isTrue();
    }

    @Test
    void getUserProfile_查看自己资料_isFollowing为false() {
        Long userId = 100L;
        Map<String, Object> profileMap = Map.of(
                "user_id", userId,
                "username", "testuser",
                "nickname", "测试用户",
                "avatar_url", null,
                "bio", null,
                "email", null,
                "created_at", null
        );

        when(userProfileRepository.getUserProfile(userId)).thenReturn(Optional.of(profileMap));
        when(userProfileRepository.getUserLikeCount(userId)).thenReturn(0L);
        when(followRepository.getFollowingCount(userId)).thenReturn(0L);
        when(followRepository.getFollowerCount(userId)).thenReturn(0L);

        UserPublicProfile response = userProfileService.getUserProfile(userId, userId);

        assertThat(response.isFollowing()).isFalse();
        verify(followRepository, never()).isFollowing(anyLong(), anyLong());
    }

    @Test
    void getUserProfile_用户不存在_抛出异常() {
        when(userProfileRepository.getUserProfile(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userProfileService.getUserProfile(999L, 100L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("用户不存在");
    }

    // ==================== 用户帖子列表测试 ====================

    @Test
    void getUserPosts_用户存在_返回帖子列表() {
        Long userId = 100L;
        Map<String, Object> profileMap = Map.of("user_id", userId);
        List<PostResponse> mockPosts = List.of(
                new PostResponse(1L, userId, "测试用户", "标题1", "预览1", "标签1", 10, 5, 3, 2, LocalDateTime.now())
        );

        when(userProfileRepository.getUserProfile(userId)).thenReturn(Optional.of(profileMap));
        when(userProfileRepository.getUserPosts(userId, 0, 20)).thenReturn(mockPosts);
        when(userProfileRepository.getUserPostCount(userId)).thenReturn(1L);

        UserPostsResponse response = userProfileService.getUserPosts(userId, 1, 20);

        assertThat(response.items()).hasSize(1);
        assertThat(response.page()).isEqualTo(1);
        assertThat(response.total()).isEqualTo(1);
        assertThat(response.hasMore()).isFalse();
    }

    @Test
    void getUserPosts_用户不存在_抛出异常() {
        when(userProfileRepository.getUserProfile(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userProfileService.getUserPosts(999L, 1, 20))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("用户不存在");
    }

    @Test
    void getUserPosts_参数边界处理_page为0设为1() {
        Long userId = 100L;
        when(userProfileRepository.getUserProfile(userId)).thenReturn(Optional.of(Map.of("user_id", userId)));
        when(userProfileRepository.getUserPosts(userId, 0, 20)).thenReturn(List.of());
        when(userProfileRepository.getUserPostCount(userId)).thenReturn(0L);

        userProfileService.getUserPosts(userId, 0, 20);

        verify(userProfileRepository).getUserPosts(eq(userId), eq(0), eq(20));
    }

    @Test
    void getUserPosts_参数边界处理_pageSize超过100设为100() {
        Long userId = 100L;
        when(userProfileRepository.getUserProfile(userId)).thenReturn(Optional.of(Map.of("user_id", userId)));
        when(userProfileRepository.getUserPosts(userId, 0, 100)).thenReturn(List.of());
        when(userProfileRepository.getUserPostCount(userId)).thenReturn(0L);

        userProfileService.getUserPosts(userId, 1, 200);

        verify(userProfileRepository).getUserPosts(eq(userId), eq(0), eq(100));
    }
}