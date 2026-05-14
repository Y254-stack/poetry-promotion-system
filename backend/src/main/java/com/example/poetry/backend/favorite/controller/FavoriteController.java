package com.example.poetry.backend.favorite.controller;

import com.example.poetry.backend.favorite.dto.FavoriteListResponse;
import com.example.poetry.backend.favorite.dto.PostCollectListResponse;
import com.example.poetry.backend.favorite.service.FavoriteService;
import com.example.poetry.backend.user.service.AuthService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@Validated
@RestController
@RequestMapping("/api/favorites")
public class FavoriteController {

    private final FavoriteService favoriteService;
    private final AuthService authService;

    public FavoriteController(FavoriteService favoriteService, AuthService authService) {
        this.favoriteService = favoriteService;
        this.authService = authService;
    }

    @GetMapping("/check")
    public Map<String, Boolean> checkFavorite(
        @RequestParam Long userId,
        @RequestParam Long workId
    ) {
        boolean isFavorited = favoriteService.isFavorited(userId, workId);
        return Map.of("isFavorited", isFavorited);
    }

    @PostMapping
    public Map<String, Boolean> addFavorite(
        @RequestParam Long userId,
        @RequestParam Long workId
    ) {
        boolean success = favoriteService.addFavorite(userId, workId);
        if (!success) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Already favorited");
        }
        return Map.of("success", true);
    }

    @DeleteMapping
    public Map<String, Boolean> removeFavorite(
        @RequestParam Long userId,
        @RequestParam Long workId
    ) {
        boolean success = favoriteService.removeFavorite(userId, workId);
        if (!success) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Favorite not found");
        }
        return Map.of("success", true);
    }

    @GetMapping
    public FavoriteListResponse getFavoriteList(
        @RequestParam Long userId,
        @RequestParam(defaultValue = "1") @Min(1) int page,
        @RequestParam(defaultValue = "20") @Min(1) @Max(50) int pageSize,
        @RequestParam(required = false) String query
    ) {
        return favoriteService.getFavoriteList(userId, page, pageSize, query);
    }

    @GetMapping("/me")
    public FavoriteListResponse getMyFavoriteList(
        @RequestHeader(value = "Authorization", required = false) String authorization,
        @RequestParam(defaultValue = "1") @Min(1) int page,
        @RequestParam(defaultValue = "20") @Min(1) @Max(50) int pageSize,
        @RequestParam(required = false) String query
    ) {
        long userId = authService.requireUserId(authorization);
        return favoriteService.getFavoriteList(userId, page, pageSize, query);
    }

    @GetMapping("/me/check")
    public Map<String, Boolean> checkMyFavorite(
        @RequestHeader(value = "Authorization", required = false) String authorization,
        @RequestParam Long workId
    ) {
        long userId = authService.requireUserId(authorization);
        boolean isFavorited = favoriteService.isFavorited(userId, workId);
        return Map.of("isFavorited", isFavorited);
    }

    @PostMapping("/me")
    public Map<String, Boolean> addMyFavorite(
        @RequestHeader(value = "Authorization", required = false) String authorization,
        @RequestParam Long workId
    ) {
        long userId = authService.requireUserId(authorization);
        boolean success = favoriteService.addFavorite(userId, workId);
        if (!success) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Already favorited");
        }
        return Map.of("success", true);
    }

    @DeleteMapping("/me")
    public Map<String, Boolean> removeMyFavorite(
        @RequestHeader(value = "Authorization", required = false) String authorization,
        @RequestParam Long workId
    ) {
        long userId = authService.requireUserId(authorization);
        boolean success = favoriteService.removeFavorite(userId, workId);
        if (!success) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Favorite not found");
        }
        return Map.of("success", true);
    }

    @GetMapping("/posts/me")
    public PostCollectListResponse getMyPostCollectList(
        @RequestHeader(value = "Authorization", required = false) String authorization,
        @RequestParam(defaultValue = "1") @Min(1) int page,
        @RequestParam(defaultValue = "20") @Min(1) @Max(50) int pageSize,
        @RequestParam(required = false) String query
    ) {
        long userId = authService.requireUserId(authorization);
        return favoriteService.getPostCollectList(userId, page, pageSize, query);
    }

    @DeleteMapping("/posts/me")
    public Map<String, Boolean> removeMyPostCollect(
        @RequestHeader(value = "Authorization", required = false) String authorization,
        @RequestParam Long postId
    ) {
        long userId = authService.requireUserId(authorization);
        boolean success = favoriteService.removePostCollect(userId, postId);
        if (!success) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Collect not found");
        }
        return Map.of("success", true);
    }
}
