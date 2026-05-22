package com.example.poetry.backend.community.controller;

import com.example.poetry.backend.community.dto.UserPostsResponse;
import com.example.poetry.backend.community.dto.UserPublicProfile;
import com.example.poetry.backend.community.dto.PostResponse;
import com.example.poetry.backend.community.service.UserProfileService;
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

@WebMvcTest(UserProfileController.class)
class UserProfileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserProfileService userProfileService;

    // ==================== 用户资料测试 ====================

    @Test
    void getUserProfile_成功_返回用户资料() throws Exception {
        UserPublicProfile profile = new UserPublicProfile(
                100L, "testuser", "测试用户", "avatar.jpg", "这是我的简介",
                "test@example.com", 15L, 20L, 50L, false, LocalDateTime.now()
        );

        when(userProfileService.getUserProfile(100L, 200L)).thenReturn(profile);

        mockMvc.perform(get("/api/profile")
                        .param("userId", "100")
                        .param("currentUserId", "200"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(100))
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.nickname").value("测试用户"))
                .andExpect(jsonPath("$.avatarUrl").value("avatar.jpg"))
                .andExpect(jsonPath("$.bio").value("这是我的简介"))
                .andExpect(jsonPath("$.likeCount").value(15))
                .andExpect(jsonPath("$.followingCount").value(20))
                .andExpect(jsonPath("$.followerCount").value(50))
                .andExpect(jsonPath("$.isFollowing").value(false));
    }

    @Test
    void getUserProfile_未登录_不传currentUserId() throws Exception {
        UserPublicProfile profile = new UserPublicProfile(
                100L, "testuser", "测试用户", "avatar.jpg", "简介",
                null, 10L, 5L, 8L, false, LocalDateTime.now()
        );

        when(userProfileService.getUserProfile(100L, null)).thenReturn(profile);

        mockMvc.perform(get("/api/profile")
                        .param("userId", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isFollowing").value(false));
    }

    // ==================== 用户帖子列表测试 ====================

    @Test
    void getUserPosts_成功_返回帖子列表() throws Exception {
        List<PostResponse> mockPosts = List.of(
                new PostResponse(1L, 100L, "测试用户", "标题1", "预览1", "标签1", 10, 5, 3, 2, LocalDateTime.now()),
                new PostResponse(2L, 100L, "测试用户", "标题2", "预览2", "标签2", 8, 4, 2, 1, LocalDateTime.now())
        );
        UserPostsResponse response = new UserPostsResponse(mockPosts, 1, 20, 2, false);

        when(userProfileService.getUserPosts(100L, 1, 20)).thenReturn(response);

        mockMvc.perform(get("/api/profile/posts")
                        .param("userId", "100")
                        .param("page", "1")
                        .param("pageSize", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.total").value(2));
    }

    // ==================== 用户统计测试 ====================

    @Test
    void getUserStats_成功_返回统计数据() throws Exception {
        UserPublicProfile profile = new UserPublicProfile(
                100L, "testuser", "测试用户", null, null,
                null, 15L, 20L, 50L, false, null
        );

        when(userProfileService.getUserProfile(100L, null)).thenReturn(profile);

        mockMvc.perform(get("/api/profile/stats")
                        .param("userId", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.likeCount").value(15))
                .andExpect(jsonPath("$.followingCount").value(20))
                .andExpect(jsonPath("$.followerCount").value(50));
    }
}