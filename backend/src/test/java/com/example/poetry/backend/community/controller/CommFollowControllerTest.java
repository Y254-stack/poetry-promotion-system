package com.example.poetry.backend.community.controller;

import com.example.poetry.backend.community.dto.FollowingListResponse;
import com.example.poetry.backend.community.dto.FollowResponse;
import com.example.poetry.backend.community.service.CommFollowService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CommFollowController.class)
class CommFollowControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CommFollowService commFollowService;

    // ==================== 关注用户测试 ====================

    @Test
    void followUser_成功_返回关注结果() throws Exception {
        FollowResponse response = new FollowResponse(true, true, 1L, 5L, "关注成功");

        when(commFollowService.followUser(100L, 200L)).thenReturn(response);

        mockMvc.perform(post("/api/follow")
                        .param("userId", "100")
                        .param("targetUserId", "200"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.isFollowing").value(true))
                .andExpect(jsonPath("$.followingCount").value(1))
                .andExpect(jsonPath("$.followerCount").value(5))
                .andExpect(jsonPath("$.message").value("关注成功"));
    }

    @Test
    void followUser_取消关注_返回取消结果() throws Exception {
        FollowResponse response = new FollowResponse(true, false, 0L, 4L, "已取消关注");

        when(commFollowService.followUser(100L, 200L)).thenReturn(response);

        mockMvc.perform(post("/api/follow")
                        .param("userId", "100")
                        .param("targetUserId", "200"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isFollowing").value(false))
                .andExpect(jsonPath("$.message").value("已取消关注"));
    }

    // ==================== 检查关注状态测试 ====================

    @Test
    void isFollowing_已关注_返回true() throws Exception {
        when(commFollowService.isFollowing(100L, 200L)).thenReturn(true);

        mockMvc.perform(get("/api/follow/status")
                        .param("userId", "100")
                        .param("targetUserId", "200"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isFollowing").value(true));
    }

    @Test
    void isFollowing_未关注_返回false() throws Exception {
        when(commFollowService.isFollowing(100L, 200L)).thenReturn(false);

        mockMvc.perform(get("/api/follow/status")
                        .param("userId", "100")
                        .param("targetUserId", "200"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isFollowing").value(false));
    }

    // ==================== 关注列表测试 ====================

    @Test
    void getFollowingList_成功_返回列表() throws Exception {
        List<FollowingListResponse.UserBriefInfo> mockList = List.of(
                new FollowingListResponse.UserBriefInfo(200L, "用户A", "avatar1.jpg", "简介1"),
                new FollowingListResponse.UserBriefInfo(201L, "用户B", "avatar2.jpg", "简介2")
        );
        FollowingListResponse response = new FollowingListResponse(mockList, 1, 20, 2, false);

        when(commFollowService.getFollowingList(100L, 1, 20)).thenReturn(response);

        mockMvc.perform(get("/api/follow/following")
                        .param("userId", "100")
                        .param("page", "1")
                        .param("pageSize", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(jsonPath("$.items[0].nickname").value("用户A"));
    }

    // ==================== 粉丝列表测试 ====================

    @Test
    void getFollowerList_成功_返回列表() throws Exception {
        List<FollowingListResponse.UserBriefInfo> mockList = List.of(
                new FollowingListResponse.UserBriefInfo(300L, "粉丝A", "fan1.jpg", "粉丝简介")
        );
        FollowingListResponse response = new FollowingListResponse(mockList, 1, 20, 1, false);

        when(commFollowService.getFollowerList(100L, 1, 20)).thenReturn(response);

        mockMvc.perform(get("/api/follow/followers")
                        .param("userId", "100")
                        .param("page", "1")
                        .param("pageSize", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].nickname").value("粉丝A"));
    }

    // ==================== 获取数量测试 ====================

    @Test
    void getFollowingCount_成功_返回数量() throws Exception {
        when(commFollowService.getFollowingCount(100L)).thenReturn(10L);

        mockMvc.perform(get("/api/follow/following-count")
                        .param("userId", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(10));
    }

    @Test
    void getFollowerCount_成功_返回数量() throws Exception {
        when(commFollowService.getFollowerCount(100L)).thenReturn(25L);

        mockMvc.perform(get("/api/follow/follower-count")
                        .param("userId", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(25));
    }
}