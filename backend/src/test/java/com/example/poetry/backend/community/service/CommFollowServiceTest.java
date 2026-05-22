package com.example.poetry.backend.community.service;

import com.example.poetry.backend.community.dto.FollowingListResponse;
import com.example.poetry.backend.community.dto.FollowResponse;
import com.example.poetry.backend.community.repository.FollowRepository;
import com.example.poetry.backend.user.repository.UserAccount;
import com.example.poetry.backend.user.repository.UserAuthRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CommFollowServiceTest {

    @Mock
    private FollowRepository followRepository;

    @Mock
    private UserAuthRepository userAuthRepository;

    @InjectMocks
    private CommFollowService commFollowService;

    // ==================== 关注/取消关注测试 ====================

    @Test
    void followUser_未关注_关注成功() {
        Long userId = 100L;
        Long targetUserId = 200L;
        UserAccount targetUser = new UserAccount(200L, "target", "pwd", "目标用户", "email", null, null, 0, null);

        when(userAuthRepository.findByUserId(targetUserId)).thenReturn(Optional.of(targetUser));
        when(followRepository.isFollowing(userId, targetUserId)).thenReturn(false);
        when(followRepository.followUser(userId, targetUserId)).thenReturn(true);
        when(followRepository.getFollowingCount(userId)).thenReturn(1L);
        when(followRepository.getFollowerCount(targetUserId)).thenReturn(5L);

        FollowResponse response = commFollowService.followUser(userId, targetUserId);

        assertThat(response.success()).isTrue();
        assertThat(response.isFollowing()).isTrue();
        assertThat(response.followingCount()).isEqualTo(1);
        assertThat(response.followerCount()).isEqualTo(5);
        assertThat(response.message()).isEqualTo("关注成功");
    }

    @Test
    void followUser_已关注_取消关注() {
        Long userId = 100L;
        Long targetUserId = 200L;
        UserAccount targetUser = new UserAccount(200L, "target", "pwd", "目标用户", "email", null, null, 0, null);

        when(userAuthRepository.findByUserId(targetUserId)).thenReturn(Optional.of(targetUser));
        when(followRepository.isFollowing(userId, targetUserId)).thenReturn(true);
        when(followRepository.unfollowUser(userId, targetUserId)).thenReturn(true);
        when(followRepository.getFollowingCount(userId)).thenReturn(0L);
        when(followRepository.getFollowerCount(targetUserId)).thenReturn(4L);

        FollowResponse response = commFollowService.followUser(userId, targetUserId);

        assertThat(response.success()).isTrue();
        assertThat(response.isFollowing()).isFalse();
        assertThat(response.followingCount()).isEqualTo(0);
        assertThat(response.followerCount()).isEqualTo(4);
        assertThat(response.message()).isEqualTo("已取消关注");
    }

    @Test
    void followUser_关注自己_抛出异常() {
        Long userId = 100L;

        assertThatThrownBy(() -> commFollowService.followUser(userId, userId))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("不能关注自己");
    }

    @Test
    void followUser_目标用户不存在_抛出异常() {
        when(userAuthRepository.findByUserId(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> commFollowService.followUser(100L, 999L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("目标用户不存在");
    }

    @Test
    void isFollowing_已关注_返回true() {
        when(followRepository.isFollowing(100L, 200L)).thenReturn(true);

        boolean result = commFollowService.isFollowing(100L, 200L);

        assertThat(result).isTrue();
    }

    @Test
    void isFollowing_未关注_返回false() {
        when(followRepository.isFollowing(100L, 200L)).thenReturn(false);

        boolean result = commFollowService.isFollowing(100L, 200L);

        assertThat(result).isFalse();
    }

    // ==================== 关注列表测试 ====================

    @Test
    void getFollowingList_正常分页_返回列表() {
        List<Map<String, Object>> mockList = List.of(
                Map.of("user_id", 200L, "nickname", "用户A", "avatar_url", "avatar1.jpg", "bio", "简介1"),
                Map.of("user_id", 201L, "nickname", "用户B", "avatar_url", "avatar2.jpg", "bio", "简介2")
        );

        when(followRepository.getFollowingList(100L, 0, 20)).thenReturn(mockList);
        when(followRepository.getFollowingCount(100L)).thenReturn(2L);

        FollowingListResponse response = commFollowService.getFollowingList(100L, 1, 20);

        assertThat(response.items()).hasSize(2);
        assertThat(response.page()).isEqualTo(1);
        assertThat(response.hasMore()).isFalse();
        assertThat(response.items().get(0).userId()).isEqualTo(200L);
        assertThat(response.items().get(0).nickname()).isEqualTo("用户A");
    }

    @Test
    void getFollowingList_有更多数据_hasMore为true() {
        when(followRepository.getFollowingList(100L, 0, 20)).thenReturn(List.of());
        when(followRepository.getFollowingCount(100L)).thenReturn(25L);

        FollowingListResponse response = commFollowService.getFollowingList(100L, 1, 20);

        assertThat(response.hasMore()).isTrue();
    }

    @Test
    void getFollowingList_参数边界处理_page为0设为1() {
        when(followRepository.getFollowingList(anyLong(), eq(0), eq(20))).thenReturn(List.of());
        when(followRepository.getFollowingCount(anyLong())).thenReturn(0L);

        commFollowService.getFollowingList(100L, 0, 20);

        verify(followRepository).getFollowingList(anyLong(), eq(0), eq(20));
    }

    @Test
    void getFollowingList_参数边界处理_pageSize超过100设为100() {
        when(followRepository.getFollowingList(anyLong(), eq(0), eq(100))).thenReturn(List.of());
        when(followRepository.getFollowingCount(anyLong())).thenReturn(0L);

        commFollowService.getFollowingList(100L, 1, 200);

        verify(followRepository).getFollowingList(anyLong(), eq(0), eq(100));
    }

    // ==================== 粉丝列表测试 ====================

    @Test
    void getFollowerList_正常分页_返回列表() {
        List<Map<String, Object>> mockList = List.of(
                Map.of("user_id", 300L, "nickname", "粉丝A", "avatar_url", "fan1.jpg", "bio", "粉丝简介")
        );

        when(followRepository.getFollowerList(100L, 0, 20)).thenReturn(mockList);
        when(followRepository.getFollowerCount(100L)).thenReturn(1L);

        FollowingListResponse response = commFollowService.getFollowerList(100L, 1, 20);

        assertThat(response.items()).hasSize(1);
        assertThat(response.items().get(0).nickname()).isEqualTo("粉丝A");
    }

    // ==================== 数量统计测试 ====================

    @Test
    void getFollowingCount_成功_返回数量() {
        when(followRepository.getFollowingCount(100L)).thenReturn(10L);

        long count = commFollowService.getFollowingCount(100L);

        assertThat(count).isEqualTo(10);
    }

    @Test
    void getFollowerCount_成功_返回数量() {
        when(followRepository.getFollowerCount(100L)).thenReturn(25L);

        long count = commFollowService.getFollowerCount(100L);

        assertThat(count).isEqualTo(25);
    }
}