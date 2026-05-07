package com.example.poetry.backend.category.controller;

import com.example.poetry.backend.category.dto.AuthorDto;
import com.example.poetry.backend.category.dto.DynastyDto;
import com.example.poetry.backend.category.repository.CategoryRepository;
import com.example.poetry.backend.tagsearch.dto.PoemTitleSearchResponse;
import com.example.poetry.backend.tagsearch.service.TagSearchService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    private final CategoryRepository categoryRepository;
    private final TagSearchService tagSearchService;

    public CategoryController(CategoryRepository categoryRepository, TagSearchService tagSearchService) {
        this.categoryRepository = categoryRepository;
        this.tagSearchService = tagSearchService;
    }

    @GetMapping("/dynasties")
    public List<DynastyDto> getDynasties() {
        return categoryRepository.getAllDynasties();
    }

    @GetMapping("/authors")
    public List<AuthorDto> getAuthors() {
        return categoryRepository.getAllAuthors();
    }

    @GetMapping("/poems/by-dynasty")
    public PoemTitleSearchResponse getPoemsByDynasty(
        @RequestParam String dynastyName,
        @RequestParam(defaultValue = "1") @Min(1) int page,
        @RequestParam(defaultValue = "20") @Min(1) @Max(50) int pageSize
    ) {
        return categoryRepository.getPoemsByDynasty(dynastyName, page, pageSize);
    }

    @GetMapping("/poems/by-author")
    public PoemTitleSearchResponse getPoemsByAuthor(
        @RequestParam Long authorId,
        @RequestParam(defaultValue = "1") @Min(1) int page,
        @RequestParam(defaultValue = "20") @Min(1) @Max(50) int pageSize
    ) {
        return categoryRepository.getPoemsByAuthorId(authorId, page, pageSize);
    }
}
