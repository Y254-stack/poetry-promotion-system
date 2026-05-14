package com.example.poetry.backend.community.service;

import com.example.poetry.backend.community.dto.FollowingListResponse;
import com.example.poetry.backend.community.dto.FollowResponse;
import com.example.poetry.backend.community.repository.FollowRepository;
import com.example.poetry.backend.user.repository.UserAuthRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CommFollowService {

    private final FollowRepository followRepository;
    private final UserAuthRepository userAuthRepository;

    public CommFollowService(FollowRepository followRepository, UserAuthRepository userAuthRepository) {
        this.followRepository = followRepository;
        this.userAuthRepository = userAuthRepository;
    }

    /**
     * 关注/取消关注用户
     */
    @Transactional
    public FollowResponse followUser(Long userId, Long targetUserId) {
        // 不能关注自己
        if (userId.equals(targetUserId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "不能关注自己");
        }

        // 验证目标用户是否存在
        userAuthRepository.findByUserId(targetUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "目标用户不存在"));

        boolean wasFollowing = followRepository.isFollowing(userId, targetUserId);
        System.out.println("DEBUG: CommFollowService - followUser: userId=" + userId + ", targetUserId=" + targetUserId + ", wasFollowing=" + wasFollowing);

        if (wasFollowing) {
            // 取消关注
            boolean result = followRepository.unfollowUser(userId, targetUserId);
            System.out.println("DEBUG: CommFollowService - unfollowUser result: " + result);
            long followingCount = followRepository.getFollowingCount(userId);
            long followerCount = followRepository.getFollowerCount(targetUserId);
            System.out.println("DEBUG: CommFollowService - after unfollow: followingCount=" + followingCount + ", followerCount=" + followerCount);
            return new FollowResponse(true, false, followingCount, followerCount, "已取消关注");
        } else {
            // 关注
            boolean result = followRepository.followUser(userId, targetUserId);
            System.out.println("DEBUG: CommFollowService - followUser result: " + result);
            long followingCount = followRepository.getFollowingCount(userId);
            long followerCount = followRepository.getFollowerCount(targetUserId);
            System.out.println("DEBUG: CommFollowService - after follow: followingCount=" + followingCount + ", followerCount=" + followerCount);
            return new FollowResponse(true, true, followingCount, followerCount, "关注成功");
        }
    }

    /**
     * 检查是否已关注某用户
     */
    public boolean isFollowing(Long userId, Long targetUserId) {
        return followRepository.isFollowing(userId, targetUserId);
    }

    /**
     * 获取用户关注列表
     */
    public FollowingListResponse getFollowingList(Long userId, int page, int pageSize) {
        if (page < 1) page = 1;
        if (pageSize < 1) pageSize = 20;
        if (pageSize > 100) pageSize = 100;

        int offset = (page - 1) * pageSize;
        List<Map<String, Object>> rawList = followRepository.getFollowingList(userId, offset, pageSize);
        long total = followRepository.getFollowingCount(userId);
        boolean hasMore = (long) offset + pageSize < total;

        List<FollowingListResponse.UserBriefInfo> items = rawList.stream()
                .map(map -> new FollowingListResponse.UserBriefInfo(
                        ((Number) map.get("user_id")).longValue(),
                        (String) map.get("nickname"),
                        (String) map.get("avatar_url"),
                        (String) map.get("bio")
                ))
                .collect(Collectors.toList());

        return new FollowingListResponse(items, page, pageSize, total, hasMore);
    }

    /**
     * 获取用户粉丝列表
     */
    public FollowingListResponse getFollowerList(Long userId, int page, int pageSize) {
        if (page < 1) page = 1;
        if (pageSize < 1) pageSize = 20;
        if (pageSize > 100) pageSize = 100;

        int offset = (page - 1) * pageSize;
        List<Map<String, Object>> rawList = followRepository.getFollowerList(userId, offset, pageSize);
        long total = followRepository.getFollowerCount(userId);
        boolean hasMore = (long) offset + pageSize < total;

        List<FollowingListResponse.UserBriefInfo> items = rawList.stream()
                .map(map -> new FollowingListResponse.UserBriefInfo(
                        ((Number) map.get("user_id")).longValue(),
                        (String) map.get("nickname"),
                        (String) map.get("avatar_url"),
                        (String) map.get("bio")
                ))
                .collect(Collectors.toList());

        return new FollowingListResponse(items, page, pageSize, total, hasMore);
    }

    /**
     * 获取关注数量
     */
    public long getFollowingCount(Long userId) {
        return followRepository.getFollowingCount(userId);
    }

    /**
     * 获取粉丝数量
     */
    public long getFollowerCount(Long userId) {
        return followRepository.getFollowerCount(userId);
    }
}