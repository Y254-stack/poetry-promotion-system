package com.example.poetry.backend.usercenter.controller;

import com.example.poetry.backend.common.dto.OkResponse;
import com.example.poetry.backend.common.dto.PagedResponse;
import com.example.poetry.backend.user.security.JwtTokenProvider;
import com.example.poetry.backend.usercenter.dto.FavoritePoemDto;
import com.example.poetry.backend.usercenter.dto.FavoritePostDto;
import com.example.poetry.backend.usercenter.dto.FollowedUserDto;
import com.example.poetry.backend.usercenter.service.UserCenterService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@Validated
@RestController
@RequestMapping("/api/user-center")
public class UserCenterController {

    private final UserCenterService userCenterService;
    private final JwtTokenProvider jwtTokenProvider;

    public UserCenterController(UserCenterService userCenterService, JwtTokenProvider jwtTokenProvider) {
        this.userCenterService = userCenterService;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @GetMapping("/follows")
    public PagedResponse<FollowedUserDto> getMyFollows(
        @RequestHeader(value = "Authorization", required = false) String authorization,
        @RequestParam(defaultValue = "1") @Min(1) int page,
        @RequestParam(defaultValue = "20") @Min(1) @Max(50) int pageSize
    ) {
        Long userId = requireUserId(authorization);
        return userCenterService.getFollowedUsers(userId, page, pageSize);
    }

    @DeleteMapping("/follows/{followedUserId}")
    public OkResponse unfollow(
        @RequestHeader(value = "Authorization", required = false) String authorization,
        @PathVariable Long followedUserId
    ) {
        Long userId = requireUserId(authorization);
        boolean ok = userCenterService.unfollow(userId, followedUserId);
        if (!ok) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "not followed");
        }
        return OkResponse.success();
    }

    @GetMapping("/favorites/poems")
    public PagedResponse<FavoritePoemDto> getMyFavoritePoems(
        @RequestHeader(value = "Authorization", required = false) String authorization,
        @RequestParam(defaultValue = "1") @Min(1) int page,
        @RequestParam(defaultValue = "20") @Min(1) @Max(50) int pageSize
    ) {
        Long userId = requireUserId(authorization);
        return userCenterService.getFavoritePoems(userId, page, pageSize);
    }

    @DeleteMapping("/favorites/poems/{workId}")
    public OkResponse unfavoritePoem(
        @RequestHeader(value = "Authorization", required = false) String authorization,
        @PathVariable Long workId
    ) {
        Long userId = requireUserId(authorization);
        boolean ok = userCenterService.unfavoritePoem(userId, workId);
        if (!ok) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "not favorited");
        }
        return OkResponse.success();
    }

    @GetMapping("/favorites/posts")
    public PagedResponse<FavoritePostDto> getMyFavoritePosts(
        @RequestHeader(value = "Authorization", required = false) String authorization,
        @RequestParam(defaultValue = "1") @Min(1) int page,
        @RequestParam(defaultValue = "20") @Min(1) @Max(50) int pageSize
    ) {
        Long userId = requireUserId(authorization);
        return userCenterService.getFavoritePosts(userId, page, pageSize);
    }

    @DeleteMapping("/favorites/posts/{postId}")
    public OkResponse unfavoritePost(
        @RequestHeader(value = "Authorization", required = false) String authorization,
        @PathVariable Long postId
    ) {
        Long userId = requireUserId(authorization);
        boolean ok = userCenterService.unfavoritePost(userId, postId);
        if (!ok) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "not collected");
        }
        return OkResponse.success();
    }

    private Long requireUserId(String authHeader) {
        String token = extractBearerToken(authHeader);
        return jwtTokenProvider.parseUserId(token);
    }

    private String extractBearerToken(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "缺少 Bearer Token");
        }
        return authHeader.substring("Bearer ".length()).trim();
    }
}

