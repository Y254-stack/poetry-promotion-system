package com.example.poetry.backend.community.controller;

import com.example.poetry.backend.community.dto.FollowingListResponse;
import com.example.poetry.backend.community.dto.FollowResponse;
import com.example.poetry.backend.community.service.CommFollowService;
import com.example.poetry.backend.user.security.JwtTokenProvider;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/follow")
public class CommFollowController {

    private final CommFollowService commFollowService;
    private final JwtTokenProvider jwtTokenProvider;

    public CommFollowController(CommFollowService commFollowService, JwtTokenProvider jwtTokenProvider) {
        this.commFollowService = commFollowService;
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
        return commFollowService.followUser(userId, targetUserId);
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
        return commFollowService.isFollowing(userId, targetUserId);
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
        return commFollowService.getFollowingList(userId, page, pageSize);
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
        return commFollowService.getFollowerList(userId, page, pageSize);
    }

    /**
     * 获取关注数量
     */
    @GetMapping("/{targetUserId}/following-count")
    public long getFollowingCount(@PathVariable Long targetUserId) {
        return commFollowService.getFollowingCount(targetUserId);
    }

    /**
     * 获取粉丝数量
     */
    @GetMapping("/{targetUserId}/follower-count")
    public long getFollowerCount(@PathVariable Long targetUserId) {
        return commFollowService.getFollowerCount(targetUserId);
    }

    private Long extractUserId(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "缺少 Bearer Token");
        }
        String token = authorization.substring("Bearer ".length()).trim();
        return jwtTokenProvider.parseUserId(token);
    }
}