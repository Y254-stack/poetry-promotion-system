package com.example.poetry.backend.user.controller;

import com.example.poetry.backend.user.dto.UserPublicProfileResponse;
import com.example.poetry.backend.user.dto.UserPublishedPostsResponse;
import com.example.poetry.backend.user.service.UserPublicProfileService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@Validated
@RestController
@RequestMapping("/api/users")
public class UserPublicProfileController {

    private final UserPublicProfileService userPublicProfileService;

    public UserPublicProfileController(UserPublicProfileService userPublicProfileService) {
        this.userPublicProfileService = userPublicProfileService;
    }

    @GetMapping("/{userId}/public")
    public UserPublicProfileResponse publicProfile(@PathVariable long userId) {
        return userPublicProfileService.getPublicProfile(userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "用户不存在"));
    }

    @GetMapping("/{userId}/posts")
    public UserPublishedPostsResponse publishedPosts(
        @PathVariable long userId,
        @RequestParam(defaultValue = "1") @Min(1) int page,
        @RequestParam(defaultValue = "50") @Min(1) @Max(100) int pageSize
    ) {
        if (userPublicProfileService.getPublicProfile(userId).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "用户不存在");
        }
        return userPublicProfileService.listPublishedPosts(userId, page, pageSize);
    }
}
