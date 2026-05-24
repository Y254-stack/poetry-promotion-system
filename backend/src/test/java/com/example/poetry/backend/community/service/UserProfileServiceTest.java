package com.example.poetry.backend.community.service;

import com.example.poetry.backend.community.dto.PostResponse;
import com.example.poetry.backend.community.dto.UserPostsResponse;
import com.example.poetry.backend.community.dto.UserPublicProfile;
import com.example.poetry.backend.community.repository.FollowRepository;
import com.example.poetry.backend.community.repository.UserProfileRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserProfileServiceTest {

    private static final Long USER_ID = 10L;

    @Mock
    private UserProfileRepository userProfileRepository;

    @Mock
    private FollowRepository followRepository;

    @InjectMocks
    private UserProfileService userProfileService;

    // ==================== HEAD：英文命名用例 ====================

    @Test
    void getUserProfile_success_returnsProfileWithStats() {
        when(userProfileRepository.getUserProfile(USER_ID)).thenReturn(Optional.of(sampleProfileMap()));
        when(userProfileRepository.getUserLikeCount(USER_ID)).thenReturn(12L);
        when(followRepository.getFollowingCount(USER_ID)).thenReturn(3L);
        when(followRepository.getFollowerCount(USER_ID)).thenReturn(8L);
        when(followRepository.isFollowing(20L, USER_ID)).thenReturn(true);

        UserPublicProfile profile = userProfileService.getUserProfile(USER_ID, 20L);

        assertEquals(USER_ID, profile.userId());
        assertEquals("poet", profile.username());
        assertEquals("诗人", profile.nickname());
        assertEquals(12L, profile.likeCount());
        assertEquals(3L, profile.followingCount());
        assertEquals(8L, profile.followerCount());
        assertTrue(profile.isFollowing());
    }

    @Test
    void getUserProfile_userNotFound_throwsNotFound() {
        when(userProfileRepository.getUserProfile(USER_ID)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(
            ResponseStatusException.class,
            () -> userProfileService.getUserProfile(USER_ID, null)
        );

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        assertEquals("用户不存在", ex.getReason());
    }

    @Test
    void getUserProfile_selfView_isNotFollowing() {
        when(userProfileRepository.getUserProfile(USER_ID)).thenReturn(Optional.of(sampleProfileMap()));
        when(userProfileRepository.getUserLikeCount(USER_ID)).thenReturn(0L);
        when(followRepository.getFollowingCount(USER_ID)).thenReturn(0L);
        when(followRepository.getFollowerCount(USER_ID)).thenReturn(0L);

        UserPublicProfile profile = userProfileService.getUserProfile(USER_ID, USER_ID);

        assertFalse(profile.isFollowing());
    }

    @Test
    void getUserPosts_success_returnsPagedResult() {
        PostResponse post = new PostResponse(
            1L, USER_ID, "作者", "标题", "预览", "标签", 0, 0, 0, 0, LocalDateTime.parse("2024-01-01T10:00:00")
        );
        when(userProfileRepository.getUserProfile(USER_ID)).thenReturn(Optional.of(sampleProfileMap()));
        when(userProfileRepository.getUserPosts(eq(USER_ID), eq(0), eq(20))).thenReturn(List.of(post));
        when(userProfileRepository.getUserPostCount(USER_ID)).thenReturn(1L);

        UserPostsResponse response = userProfileService.getUserPosts(USER_ID, 1, 20);

        assertEquals(1, response.items().size());
        assertEquals(1, response.page());
        assertEquals(20, response.pageSize());
        assertEquals(1L, response.total());
        assertFalse(response.hasMore());
    }

    @Test
    void getUserPosts_userNotFound_throwsNotFound() {
        when(userProfileRepository.getUserProfile(USER_ID)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(
            ResponseStatusException.class,
            () -> userProfileService.getUserPosts(USER_ID, 1, 20)
        );

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    void getUserPosts_pageSizeOverLimit_capsAt100() {
        when(userProfileRepository.getUserProfile(USER_ID)).thenReturn(Optional.of(sampleProfileMap()));
        when(userProfileRepository.getUserPosts(eq(USER_ID), eq(0), eq(100))).thenReturn(List.of());
        when(userProfileRepository.getUserPostCount(USER_ID)).thenReturn(0L);

        UserPostsResponse response = userProfileService.getUserPosts(USER_ID, 0, 200);

        assertEquals(100, response.pageSize());
        verify(userProfileRepository).getUserPosts(USER_ID, 0, 100);
    }

    @Test
    void getUserPosts_invalidPage_normalizesToFirstPage() {
        when(userProfileRepository.getUserProfile(USER_ID)).thenReturn(Optional.of(sampleProfileMap()));
        when(userProfileRepository.getUserPosts(eq(USER_ID), eq(0), eq(20))).thenReturn(List.of());
        when(userProfileRepository.getUserPostCount(USER_ID)).thenReturn(0L);

        UserPostsResponse response = userProfileService.getUserPosts(USER_ID, 0, 20);

        assertEquals(1, response.page());
        verify(userProfileRepository).getUserPosts(USER_ID, 0, 20);
    }

    // ==================== develop：队友中文命名用例 ====================

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

    private Map<String, Object> sampleProfileMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("user_id", USER_ID);
        map.put("username", "poet");
        map.put("nickname", "诗人");
        map.put("avatar_url", "http://avatar");
        map.put("bio", "简介");
        map.put("email", "poet@example.com");
        map.put("created_at", "2024-01-01T10:00:00");
        return map;
    }
}
