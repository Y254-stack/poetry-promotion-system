package com.example.poetry.backend.usercenter.service;

import com.example.poetry.backend.common.dto.PagedResponse;
import com.example.poetry.backend.usercenter.dto.FavoritePoemDto;
import com.example.poetry.backend.usercenter.dto.FavoritePostDto;
import com.example.poetry.backend.usercenter.dto.FollowedUserDto;
import com.example.poetry.backend.usercenter.repository.UserCenterRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserCenterService {

    private final UserCenterRepository repository;

    public UserCenterService(UserCenterRepository repository) {
        this.repository = repository;
    }

    public PagedResponse<FollowedUserDto> getFollowedUsers(Long userId, int page, int pageSize) {
        int safePage = Math.max(page, 1);
        int safePageSize = Math.min(Math.max(pageSize, 1), 50);
        int offset = (safePage - 1) * safePageSize;

        int total = repository.countFollowedUsers(userId);
        List<FollowedUserDto> items = total == 0
            ? List.of()
            : repository.listFollowedUsers(userId, safePageSize, offset);
        return new PagedResponse<>(safePage, safePageSize, total, items);
    }

    public boolean unfollow(Long userId, Long followedUserId) {
        return repository.unfollow(userId, followedUserId) > 0;
    }

    public PagedResponse<FavoritePoemDto> getFavoritePoems(Long userId, int page, int pageSize) {
        int safePage = Math.max(page, 1);
        int safePageSize = Math.min(Math.max(pageSize, 1), 50);
        int offset = (safePage - 1) * safePageSize;

        int total = repository.countFavoritePoems(userId);
        List<FavoritePoemDto> items = total == 0
            ? List.of()
            : repository.listFavoritePoems(userId, safePageSize, offset);
        return new PagedResponse<>(safePage, safePageSize, total, items);
    }

    public boolean unfavoritePoem(Long userId, Long workId) {
        return repository.unfavoritePoem(userId, workId) > 0;
    }

    public PagedResponse<FavoritePostDto> getFavoritePosts(Long userId, int page, int pageSize) {
        int safePage = Math.max(page, 1);
        int safePageSize = Math.min(Math.max(pageSize, 1), 50);
        int offset = (safePage - 1) * safePageSize;

        int total = repository.countFavoritePosts(userId);
        List<FavoritePostDto> items = total == 0
            ? List.of()
            : repository.listFavoritePosts(userId, safePageSize, offset);
        return new PagedResponse<>(safePage, safePageSize, total, items);
    }

    @Transactional
    public boolean unfavoritePost(Long userId, Long postId) {
        int deleted = repository.unfavoritePost(userId, postId);
        if (deleted <= 0) {
            return false;
        }
        repository.decrementPostCollectCount(postId);
        return true;
    }
}

