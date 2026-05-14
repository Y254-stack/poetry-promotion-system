package com.example.poetry.backend.follow.service;

import com.example.poetry.backend.follow.dto.FollowListItemDto;
import com.example.poetry.backend.follow.dto.FollowListResponse;
import com.example.poetry.backend.follow.repository.UserFollowRepository;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class FollowService {

    private final UserFollowRepository userFollowRepository;

    public FollowService(UserFollowRepository userFollowRepository) {
        this.userFollowRepository = userFollowRepository;
    }

    public FollowListResponse listMyFollowing(long followerUserId, int page, int pageSize) {
        int total = userFollowRepository.countFollowing(followerUserId);
        List<FollowListItemDto> items = userFollowRepository.listFollowing(followerUserId, page, pageSize);
        return new FollowListResponse(page, pageSize, total, items);
    }

    public boolean unfollow(long followerUserId, long followedUserId) {
        int rows = userFollowRepository.deleteFollow(followerUserId, followedUserId);
        return rows > 0;
    }
}
