package com.example.poetry.backend.community.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.poetry.backend.community.dto.PostResponse;
import com.example.poetry.backend.community.dto.UserPostsResponse;
import com.example.poetry.backend.community.dto.UserPublicProfile;
import com.example.poetry.backend.community.repository.FollowRepository;
import com.example.poetry.backend.community.repository.UserProfileRepository;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class UserProfileServiceTest {

    private static final Long USER_ID = 10L;

    @Mock
    private UserProfileRepository userProfileRepository;

    @Mock
    private FollowRepository followRepository;

    private UserProfileService userProfileService;

    @BeforeEach
    void setUp() {
        userProfileService = new UserProfileService(userProfileRepository, followRepository);
    }

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
