package com.example.poetry.backend.community.controller;

import com.example.poetry.backend.community.dto.UserPostsResponse;
import com.example.poetry.backend.community.dto.UserPublicProfile;
import com.example.poetry.backend.community.service.UserProfileService;
import com.example.poetry.backend.user.security.JwtTokenProvider;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/profile")
public class UserProfileController {

    private final UserProfileService userProfileService;
    private final JwtTokenProvider jwtTokenProvider;

    public UserProfileController(UserProfileService userProfileService, JwtTokenProvider jwtTokenProvider) {
        this.userProfileService = userProfileService;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    /**
     * 获取用户公开资料
     */
    @GetMapping("/{userId}")
    public UserPublicProfile getUserProfile(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable Long userId
    ) {
        Long currentUserId = null;
        if (authorization != null && authorization.startsWith("Bearer ")) {
            String token = authorization.substring("Bearer ".length()).trim();
            try {
                currentUserId = jwtTokenProvider.parseUserId(token);
            } catch (Exception e) {
                // Token无效时忽略
            }
        }
        return userProfileService.getUserProfile(userId, currentUserId);
    }

    /**
     * 获取用户发布的帖子列表
     */
    @GetMapping("/{userId}/posts")
    public UserPostsResponse getUserPosts(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize
    ) {
        return userProfileService.getUserPosts(userId, page, pageSize);
    }

    /**
     * 获取我的个人资料（当前登录用户）
     */
    @GetMapping("/me")
    public UserPublicProfile getMyProfile(
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        Long userId = extractUserId(authorization);
        return userProfileService.getUserProfile(userId, userId);
    }
    private Long extractUserId(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "缺少 Bearer Token");
        }
        String token = authorization.substring("Bearer ".length()).trim();
        return jwtTokenProvider.parseUserId(token);
    }
}