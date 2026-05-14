package com.example.poetry.backend.follow.controller;

import com.example.poetry.backend.follow.dto.FollowListResponse;
import com.example.poetry.backend.follow.service.FollowService;
import com.example.poetry.backend.user.service.AuthService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@Validated
@RestController
@RequestMapping("/api/follows")
public class FollowController {

    private final FollowService followService;
    private final AuthService authService;

    public FollowController(FollowService followService, AuthService authService) {
        this.followService = followService;
        this.authService = authService;
    }

    @GetMapping("/me")
    public FollowListResponse listMyFollowing(
        @RequestHeader(value = "Authorization", required = false) String authorization,
        @RequestParam(defaultValue = "1") @Min(1) int page,
        @RequestParam(defaultValue = "50") @Min(1) @Max(100) int pageSize
    ) {
        long userId = authService.requireUserId(authorization);
        return followService.listMyFollowing(userId, page, pageSize);
    }

    @DeleteMapping("/me")
    public Map<String, Boolean> unfollow(
        @RequestHeader(value = "Authorization", required = false) String authorization,
        @RequestParam long followedUserId
    ) {
        long userId = authService.requireUserId(authorization);
        boolean ok = followService.unfollow(userId, followedUserId);
        if (!ok) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "未关注该用户");
        }
        return Map.of("success", true);
    }
}
