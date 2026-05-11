package com.example.poetry.backend.community.controller;

import com.example.poetry.backend.community.dto.FollowingListResponse;
import com.example.poetry.backend.community.dto.FollowResponse;
import com.example.poetry.backend.community.service.FollowService;
import com.example.poetry.backend.user.security.JwtTokenProvider;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/follow")
public class FollowController {

    private final FollowService followService;
    private final JwtTokenProvider jwtTokenProvider;

    public FollowController(FollowService followService, JwtTokenProvider jwtTokenProvider) {
        this.followService = followService;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    /**
     * 关注/取消关注用户
     */
    @PostMapping("/{targetUserId}")
    public FollowResponse followUser(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable Long targetUserId
    ) {
        Long userId = extractUserId(authorization);
        return followService.followUser(userId, targetUserId);
    }

    /**
     * 检查是否已关注某用户
     */
    @GetMapping("/{targetUserId}/is-following")
    public boolean isFollowing(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable Long targetUserId
    ) {
        Long userId = extractUserId(authorization);
        return followService.isFollowing(userId, targetUserId);
    }

    /**
     * 获取我的关注列表
     */
    @GetMapping("/following")
    public FollowingListResponse getFollowingList(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize
    ) {
        Long userId = extractUserId(authorization);
        return followService.getFollowingList(userId, page, pageSize);
    }

    /**
     * 获取我的粉丝列表
     */
    @GetMapping("/followers")
    public FollowingListResponse getFollowerList(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize
    ) {
        Long userId = extractUserId(authorization);
        return followService.getFollowerList(userId, page, pageSize);
    }

    /**
     * 获取关注数量
     */
    @GetMapping("/{targetUserId}/following-count")
    public long getFollowingCount(@PathVariable Long targetUserId) {
        return followService.getFollowingCount(targetUserId);
    }

    /**
     * 获取粉丝数量
     */
    @GetMapping("/{targetUserId}/follower-count")
    public long getFollowerCount(@PathVariable Long targetUserId) {
        return followService.getFollowerCount(targetUserId);
    }

    private Long extractUserId(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "缺少 Bearer Token");
        }
        String token = authorization.substring("Bearer ".length()).trim();
        return jwtTokenProvider.parseUserId(token);
    }
}