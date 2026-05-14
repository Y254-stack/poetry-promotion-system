package com.example.poetry.backend.favorite.service;

import com.example.poetry.backend.favorite.dto.FavoriteListResponse;
import com.example.poetry.backend.favorite.dto.PostCollectListItemDto;
import com.example.poetry.backend.favorite.dto.PostCollectListResponse;
import com.example.poetry.backend.favorite.repository.FavoriteRepository;
import com.example.poetry.backend.favorite.repository.PostCollectRepository;
import com.example.poetry.backend.tagsearch.dto.PoemSearchItemDto;
import java.util.List;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

@Service
public class FavoriteService {

    private final FavoriteRepository favoriteRepository;
    private final PostCollectRepository postCollectRepository;

    public FavoriteService(FavoriteRepository favoriteRepository, PostCollectRepository postCollectRepository) {
        this.favoriteRepository = favoriteRepository;
        this.postCollectRepository = postCollectRepository;
    }

    public boolean isFavorited(Long userId, Long workId) {
        return favoriteRepository.isFavorited(userId, workId);
    }

    public boolean addFavorite(Long userId, Long workId) {
        try {
            int rows = favoriteRepository.addFavorite(userId, workId);
            return rows > 0;
        } catch (DuplicateKeyException e) {
            return false;
        }
    }

    public boolean removeFavorite(Long userId, Long workId) {
        int rows = favoriteRepository.removeFavorite(userId, workId);
        return rows > 0;
    }

    public FavoriteListResponse getFavoriteList(Long userId, int page, int pageSize) {
        return getFavoriteList(userId, page, pageSize, null);
    }

    public FavoriteListResponse getFavoriteList(Long userId, int page, int pageSize, String query) {
        String q = query == null ? null : query.trim();
        if (q != null && q.isEmpty()) {
            q = null;
        }
        int total = favoriteRepository.countFavorites(userId, q);
        List<PoemSearchItemDto> items = favoriteRepository.getFavoriteList(userId, page, pageSize, q);
        return new FavoriteListResponse(page, pageSize, total, items);
    }

    public PostCollectListResponse getPostCollectList(Long userId, int page, int pageSize, String query) {
        String q = query == null ? null : query.trim();
        if (q != null && q.isEmpty()) {
            q = null;
        }
        int total = postCollectRepository.countCollects(userId, q);
        List<PostCollectListItemDto> items = postCollectRepository.getCollectList(userId, page, pageSize, q);
        return new PostCollectListResponse(page, pageSize, total, items);
    }

    public boolean removePostCollect(Long userId, Long postId) {
        int rows = postCollectRepository.removeCollect(userId, postId);
        return rows > 0;
    }
}
