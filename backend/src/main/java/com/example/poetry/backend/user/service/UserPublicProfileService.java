package com.example.poetry.backend.user.service;

import com.example.poetry.backend.user.dto.UserPublicProfileResponse;
import com.example.poetry.backend.user.dto.UserPublishedPostItemDto;
import com.example.poetry.backend.user.dto.UserPublishedPostsResponse;
import com.example.poetry.backend.user.repository.UserPublicProfileRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class UserPublicProfileService {

    private final UserPublicProfileRepository userPublicProfileRepository;

    public UserPublicProfileService(UserPublicProfileRepository userPublicProfileRepository) {
        this.userPublicProfileRepository = userPublicProfileRepository;
    }

    public Optional<UserPublicProfileResponse> getPublicProfile(long userId) {
        return userPublicProfileRepository.findPublicProfile(userId);
    }

    public UserPublishedPostsResponse listPublishedPosts(long userId, int page, int pageSize) {
        int total = userPublicProfileRepository.countPublishedPosts(userId);
        List<UserPublishedPostItemDto> items = userPublicProfileRepository.listPublishedPosts(userId, page, pageSize);
        return new UserPublishedPostsResponse(page, pageSize, total, items);
    }
}
