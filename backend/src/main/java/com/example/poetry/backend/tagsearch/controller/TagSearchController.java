package com.example.poetry.backend.tagsearch.controller;

import com.example.poetry.backend.tagsearch.dto.PoemDetailDto;
import com.example.poetry.backend.tagsearch.dto.PoemTagSearchResponse;
import com.example.poetry.backend.tagsearch.dto.PoemTitleSearchResponse;
import com.example.poetry.backend.tagsearch.dto.TagDto;
import com.example.poetry.backend.tagsearch.service.TagSearchService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;
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
@RequestMapping("/api")
public class TagSearchController {

    private final TagSearchService tagSearchService;

    public TagSearchController(TagSearchService tagSearchService) {
        this.tagSearchService = tagSearchService;
    }

    @GetMapping("/tags/hot")
    public List<TagDto> getHotTags(
        @RequestParam(defaultValue = "20") @Min(1) @Max(50) int limit
    ) {
        return tagSearchService.getHotTags(limit);
    }

    @GetMapping("/poems/search/by-tags")
    public PoemTagSearchResponse searchByTags(
        @RequestParam String tagIds,
        @RequestParam(defaultValue = "hot") String sort,
        @RequestParam(defaultValue = "1") @Min(1) int page,
        @RequestParam(defaultValue = "20") @Min(1) @Max(50) int pageSize
    ) {
        List<Long> parsedTagIds = parseTagIds(tagIds);
        if (parsedTagIds.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "tagIds cannot be empty");
        }
        String safeSort = "publish_time".equalsIgnoreCase(sort) ? "publish_time" : "hot";
        return tagSearchService.searchByTags(parsedTagIds, safeSort, page, pageSize);
    }

    @GetMapping("/poems/{workId}")
    public PoemDetailDto getPoemDetail(@PathVariable Long workId) {
        PoemDetailDto detail = tagSearchService.getPoemDetail(workId);
        if (detail == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Poem not found");
        }
        return detail;
    }

    @GetMapping("/search/title")
    public PoemTitleSearchResponse searchByTitle(
        @RequestParam String query,
        @RequestParam(defaultValue = "1") @Min(1) int page,
        @RequestParam(defaultValue = "20") @Min(1) @Max(50) int pageSize
    ) {
        if (query == null || query.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "query cannot be empty");
        }
        return tagSearchService.searchByTitle(query.trim(), page, pageSize);
    }

    private List<Long> parseTagIds(String tagIds) {
        return List.of(tagIds.split(",")).stream()
            .map(String::trim)
            .filter(value -> !value.isBlank())
            .map(Long::valueOf)
            .distinct()
            .toList();
    }
}

