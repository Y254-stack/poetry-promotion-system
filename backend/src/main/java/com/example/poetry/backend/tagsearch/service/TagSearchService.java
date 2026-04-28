package com.example.poetry.backend.tagsearch.service;

import com.example.poetry.backend.tagsearch.dto.PoemDetailDto;
import com.example.poetry.backend.tagsearch.dto.PoemSearchItemDto;
import com.example.poetry.backend.tagsearch.dto.PoemTagSearchResponse;
import com.example.poetry.backend.tagsearch.dto.PoemTitleSearchResponse;
import com.example.poetry.backend.tagsearch.dto.TagDto;
import com.example.poetry.backend.tagsearch.repository.TagSearchRepository;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class TagSearchService {

    private final TagSearchRepository repository;

    public TagSearchService(TagSearchRepository repository) {
        this.repository = repository;
    }

    public List<TagDto> getHotTags(int limit) {
        return repository.findHotTags(limit);
    }

    public PoemTagSearchResponse searchByTags(List<Long> tagIds, String sort, int page, int pageSize) {
        int total = repository.countByTags(tagIds);
        List<PoemSearchItemDto> items = repository.searchByTags(tagIds, sort, page, pageSize);
        List<TagDto> recommendedTags = total == 0 ? repository.findRecommendedTags(tagIds, 8) : List.of();
        boolean hasMore = page * pageSize < total;

        return new PoemTagSearchResponse(
            tagIds,
            sort,
            page,
            pageSize,
            total,
            hasMore,
            items,
            total == 0 ? "未找到同时匹配所选标签的诗词" : null,
            recommendedTags
        );
    }

    public PoemDetailDto getPoemDetail(Long workId) {
        return repository.findPoemDetail(workId);
    }

    public PoemTitleSearchResponse searchByTitle(String query, int page, int pageSize) {
        int total = repository.countByTitle(query);
        List<PoemSearchItemDto> items = repository.searchByTitle(query, page, pageSize);

        return new PoemTitleSearchResponse(
            query,
            page,
            pageSize,
            total,
            items,
            total == 0 ? "未找到包含「" + query + "」的诗词" : null
        );
    }
}

