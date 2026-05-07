package com.example.poetry.backend.favorite.service;

import com.example.poetry.backend.favorite.dto.FavoriteListResponse;
import com.example.poetry.backend.favorite.repository.FavoriteRepository;
import com.example.poetry.backend.tagsearch.dto.PoemSearchItemDto;
import java.util.List;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

@Service
public class FavoriteService {

    private final FavoriteRepository favoriteRepository;

    public FavoriteService(FavoriteRepository favoriteRepository) {
        this.favoriteRepository = favoriteRepository;
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
        int total = favoriteRepository.countFavorites(userId);
        List<PoemSearchItemDto> items = favoriteRepository.getFavoriteList(userId, page, pageSize);
        return new FavoriteListResponse(page, pageSize, total, items);
    }
}
