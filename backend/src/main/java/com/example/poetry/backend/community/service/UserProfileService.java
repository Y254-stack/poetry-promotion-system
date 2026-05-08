package com.example.poetry.backend.community.service;

import com.example.poetry.backend.community.dto.UserPostsResponse;
import com.example.poetry.backend.community.dto.UserPublicProfile;
import com.example.poetry.backend.community.repository.FollowRepository;
import com.example.poetry.backend.community.repository.UserProfileRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@Service
public class UserProfileService {

    private final UserProfileRepository userProfileRepository;
    private final FollowRepository followRepository;

    public UserProfileService(UserProfileRepository userProfileRepository, FollowRepository followRepository) {
        this.userProfileRepository = userProfileRepository;
        this.followRepository = followRepository;
    }

    /**
     * 获取用户公开资料
     */
    public UserPublicProfile getUserProfile(Long userId, Long currentUserId) {
        Map<String, Object> profileMap = userProfileRepository.getUserProfile(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "用户不存在"));

        // 获取统计数据
        long likeCount = userProfileRepository.getUserLikeCount(userId);
        long followingCount = followRepository.getFollowingCount(userId);
        long followerCount = followRepository.getFollowerCount(userId);
        
        // 判断当前用户是否关注了该用户
        boolean isFollowing = false;
        if (currentUserId != null && !currentUserId.equals(userId)) {
            isFollowing = followRepository.isFollowing(currentUserId, userId);
        }

        return new UserPublicProfile(
                ((Number) profileMap.get("user_id")).longValue(),
                (String) profileMap.get("username"),
                (String) profileMap.get("nickname"),
                (String) profileMap.get("avatar_url"),
                (String) profileMap.get("bio"),
                profileMap.get("email") != null ? (String) profileMap.get("email") : null,
                likeCount,
                followingCount,
                followerCount,
                isFollowing,
                profileMap.get("created_at") != null 
                        ? java.time.LocalDateTime.parse(profileMap.get("created_at").toString())
                        : null
        );
    }

    /**
     * 获取用户发布的帖子列表
     */
    public UserPostsResponse getUserPosts(Long userId, int page, int pageSize) {
        // 验证用户是否存在
        if (userProfileRepository.getUserProfile(userId).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "用户不存在");
        }

        if (page < 1) page = 1;
        if (pageSize < 1) pageSize = 20;
        if (pageSize > 100) pageSize = 100;

        int offset = (page - 1) * pageSize;
        var items = userProfileRepository.getUserPosts(userId, offset, pageSize);
        long total = userProfileRepository.getUserPostCount(userId);
        boolean hasMore = (long) offset + pageSize < total;

        return new UserPostsResponse(items, page, pageSize, total, hasMore);
    }
}